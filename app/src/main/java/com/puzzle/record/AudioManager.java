package com.puzzle.record;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import com.puzzle.record.utils.PcmToWav;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioManager {
    private static final String TAG = AudioManager.class.getSimpleName();

    private static final int SAMPLING_RATE_IN_HZ = 44100;

    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * Factor by that the minimum buffer size is multiplied. The bigger the factor is the less
     * likely it is that samples will be dropped, but more memory will be used. The minimum buffer
     * size is determined by {@link AudioRecord#getMinBufferSize(int, int, int)} and depends on the
     * recording settings.
     */
    private static final int BUFFER_SIZE_FACTOR = 2;

    /**
     * Size of the buffer where the audio data is stored by Android
     */
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR;

    private AtomicBoolean isRecording = new AtomicBoolean(false);

    private AudioRecord mAudioRecord = null;
    private Thread recordingThread = null;
    private String recordFileName;
    private String finalFileName;

    private AudioTrack mAudioTrack = null;

    private AudioAttributes audioAttributes = null;

    private AudioFormat audioFormat = null;

    private Context mContext;

    private OnRecordListener mRecordListener;

    private static volatile AudioManager _instance = null;

    private AudioManager() {
    }

    public static AudioManager getInstance() {
        if (_instance == null) {
            synchronized (AudioManager.class) {
                if (_instance == null) {
                    _instance = new AudioManager();
                }
            }
        }
        return _instance;
    }

    /**
     * 初始化环境
     */
    public void init(Context context) {
        mContext = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
            audioFormat = new AudioFormat.Builder().setSampleRate(SAMPLING_RATE_IN_HZ)
                    .setEncoding(AUDIO_FORMAT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
        }
    }

    /**
     * 开始录音
     */
    public void startRecord(OnRecordListener listener) {
        mRecordListener = listener;
        initAudioRecord();
        mAudioRecord.startRecording();
        isRecording.set(true);

        recordingThread = new Thread(new RecordingRunnable(), "Recording Thread");
        recordingThread.start();
    }

    private void initAudioRecord() {
        String basePath = mContext.getExternalCacheDir() + "/record/";
        File file = new File(basePath);
        if (!file.isDirectory()) file.mkdirs();

        recordFileName = mContext.getExternalCacheDir() + "/record/" + SystemClock.uptimeMillis() + ".pcm";
        finalFileName = recordFileName.replace(".pcm", ".mp3");

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLING_RATE_IN_HZ,
                CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
    }

    private class RecordingRunnable implements Runnable {

        @Override
        public void run() {
            final File file = new File(recordFileName);
            final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            try (final FileOutputStream outStream = new FileOutputStream(file)) {
                while (isRecording.get()) {
                    int result = mAudioRecord.read(buffer, BUFFER_SIZE);
                    if (result < 0) {
                        Log.e(TAG, "Reading of audio buffer failed: " + getBufferReadFailureReason(result));
                    }
                    outStream.write(buffer.array(), 0, BUFFER_SIZE);
                    buffer.clear();
                }
            } catch (IOException e) {
                Log.e(TAG, "Writing of recorded audio failed. " + e);
            }
        }

        private String getBufferReadFailureReason(int errorCode) {
            switch (errorCode) {
                case AudioRecord.ERROR_INVALID_OPERATION:
                    return "ERROR_INVALID_OPERATION";
                case AudioRecord.ERROR_BAD_VALUE:
                    return "ERROR_BAD_VALUE";
                case AudioRecord.ERROR_DEAD_OBJECT:
                    return "ERROR_DEAD_OBJECT";
                case AudioRecord.ERROR:
                    return "ERROR";
                default:
                    return "Unknown (" + errorCode + ")";
            }
        }
    }

    /**
     * 取消录音
     * */
    public void cancelRecord() {
        if (mAudioRecord != null && mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            isRecording.set(false);
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;

            recordingThread = null;
        }
    }

    /**
     * 停止录音
     */
    public void stopRecord() {
        if (mAudioRecord == null) return;

        isRecording.set(false);
        mAudioRecord.stop();
        mAudioRecord.release();
        mAudioRecord = null;

        recordingThread = null;

        formatAudio();
    }

    private void formatAudio() {
        try {
            PcmToWav.mergePCMFilesToWAVFile(recordFileName, finalFileName);
            File file = new File(recordFileName);
            if (file.exists()) {
                file.delete();
            }

            if (mRecordListener != null) {
                mRecordListener.onRecorded(finalFileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface OnRecordListener {
        void onRecorded(String filePath);
    }

    /**
     * 开始播放
     */
    public void startPlay(String filePath) {
        initAudioTrack();
        mAudioTrack.play();

        Executors.newCachedThreadPool().execute(new Runnable() {

            @Override
            public void run() {
                FileInputStream fis = null;
                try {
                    File file = new File(filePath);
                    fis = new FileInputStream(file);

                    byte[] buffer = new byte[BUFFER_SIZE];
                    while (fis.available() > 0) {
                        int readCount = fis.read(buffer);
                        if (readCount == AudioTrack.ERROR_INVALID_OPERATION || readCount == AudioTrack.ERROR_BAD_VALUE) {
                            continue;
                        }
                        if (readCount != 0 && readCount != -1) {
                            mAudioTrack.write(buffer, 0, readCount);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void initAudioTrack() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mAudioTrack = new AudioTrack(audioAttributes, audioFormat, BUFFER_SIZE,
                    AudioTrack.MODE_STREAM, android.media.AudioManager.AUDIO_SESSION_ID_GENERATE);
        }
    }

    /**
     * 停止播放
     * */
    public void stopPlay() {
        if (mAudioTrack == null) return;

        if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
            mAudioTrack.stop();
        }
        mAudioTrack.release();
        mAudioTrack = null;
    }

}
