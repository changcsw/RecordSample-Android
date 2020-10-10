package com.puzzle.record;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioHelper {
    private static final String TAG = AudioHelper.class.getSimpleName();

    private static final int SAMPLING_RATE_IN_HZ = 44100;

    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * Size of the buffer where the audio data is stored by Android
     */
    private static final int BUFFER_SIZE_IN_BYTES = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * 2;

    private AtomicBoolean isRecording = new AtomicBoolean(false);

    private AudioRecord mAudioRecord = null;
    private Thread recordingThread = null;

    private AudioTrack mAudioTrack = null;

    private AudioAttributes audioAttributes = null;

    private AudioFormat audioFormat = null;

    private OnRecordListener mRecordListener;

    private List<byte[]> mAudioDataList = new ArrayList<>();

    private static volatile AudioHelper _instance = null;

    private AudioHelper() {
    }

    public static AudioHelper getInstance() {
        if (_instance == null) {
            synchronized (AudioHelper.class) {
                if (_instance == null) {
                    _instance = new AudioHelper();
                }
            }
        }
        return _instance;
    }

    /**
     * 开始录音
     */
    public void startRecord(OnRecordListener listener) {
        mRecordListener = listener;
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLING_RATE_IN_HZ,
                CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE_IN_BYTES);
        mAudioRecord.startRecording();
        isRecording.set(true);

        recordingThread = new Thread(new RecordingRunnable(), "Recording Thread");
        recordingThread.start();
    }

    private class RecordingRunnable implements Runnable {

        @Override
        public void run() {
            // byte[] audioData = new byte[BUFFER_SIZE_IN_BYTES];
            ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE_IN_BYTES);
            try {
                while (isRecording.get() && mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    // int result = mAudioRecord.read(audioData, 0, BUFFER_SIZE_IN_BYTES);
                    int result = mAudioRecord.read(buffer, BUFFER_SIZE_IN_BYTES);
                    if (result < 0) {
                        Log.e(TAG, "Reading of audio buffer failed: " + getBufferReadFailureReason(result));
                    }

                    mRecodingListener.onRecording(buffer.array(), 0, BUFFER_SIZE_IN_BYTES);
                    buffer.clear();
                }
            } catch (Exception e) {
                e.printStackTrace();
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

        private final OnRecordingListener mRecodingListener = new OnRecordingListener() {
            @Override
            public void onRecording(byte[] audioData, int off, int len) {
                byte[] temp = new byte[len];
                System.arraycopy(audioData, off, temp, 0, len);
                mAudioDataList.add(temp);
            }
        };
    }

    /**
     * 取消录音
     */
    public void cancelRecord() {
        if (mAudioRecord != null && mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            isRecording.set(false);
            mAudioRecord.stop();
            mAudioRecord.release();

            if (mAudioDataList != null) {
                mAudioDataList.clear();
            }

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

        formatAudioData();
    }

    private void formatAudioData() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                byte[] audioData = new byte[0];

                if (mAudioDataList != null) {
                    int len = 0;
                    for (byte[] data : mAudioDataList) {
                        len += data.length;
                    }

                    audioData = new byte[len];

                    int countLength = 0;
                    for (byte[] data: mAudioDataList) {
                        System.arraycopy(data, 0, audioData, countLength, data.length);
                        countLength += data.length;
                    }

                    mAudioDataList.clear();
                }

                if (mRecordListener != null) {
                    mRecordListener.onRecorded(audioData);
                }
                    }
        });
    }

    private interface OnRecordingListener{
        void onRecording(byte[] audioData, int off, int len);
    }

    public interface OnRecordListener {
        void onRecorded(byte[] audioData);
    }

    /**
     * 开始播放
     */
    public void startPlay(byte[] audioData, OnPlayListener playListener) {
        Executors.newCachedThreadPool().execute(new Runnable() {

            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
                    audioFormat = new AudioFormat.Builder().setSampleRate(SAMPLING_RATE_IN_HZ)
                            .setEncoding(AUDIO_FORMAT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();

                    mAudioTrack = new AudioTrack(audioAttributes, audioFormat, audioData.length,
                            AudioTrack.MODE_STATIC, android.media.AudioManager.AUDIO_SESSION_ID_GENERATE);

                    mAudioTrack.setNotificationMarkerPosition(audioData.length/2);
                    // mAudioTrack.setPositionNotificationPeriod(audioData.length/2);
                    mAudioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                        @Override
                        public void onMarkerReached(AudioTrack track) {
                            if (playListener != null) {
                                playListener.onPlayFinish();
                            }
                        }

                        @Override
                        public void onPeriodicNotification(AudioTrack track) {
                            // noting to do
                        }
                    });
                } else {
                    Log.e(TAG, "Device's OS version too low. not support AudioTrack play.");
                    return;
                }

                mAudioTrack.write(audioData, 0, audioData.length);
                mAudioTrack.play();
            }
        });
    }

    /**
     * 停止播放
     */
    public void stopPlay() {
        if (mAudioTrack == null) return;

        if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
            mAudioTrack.stop();
        }
        mAudioTrack.release();
        mAudioTrack = null;
    }

    public interface OnPlayListener {
        void onPlayFinish();
    }

}
