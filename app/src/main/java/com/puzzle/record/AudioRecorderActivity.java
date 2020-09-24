package com.puzzle.record;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.puzzle.record.databinding.ActivityAudioBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.Executors;


public class AudioRecorderActivity extends BaseActivity implements View.OnClickListener {

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

    private AudioRecord recorder = null;
    private AudioTrack audioTrack = null;
    private Thread recordingThread = null;
    private String recordFileName;

    ActivityAudioBinding mBinding;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityAudioBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        initView();
        recordFileName = Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath() + "/recording.pcm";
        initAudio();
    }

    @SuppressLint("SetTextI18n")
    private void initView() {
        mBinding.switchMediaRecorder.setText("SwitchMediaRecorder");
        mBinding.audioRecorder.setText("StartAudioRecorder");
        mBinding.audioPlayer.setText("StartAudioPlayer");

        mBinding.switchMediaRecorder.setOnClickListener(this);
        mBinding.audioRecorder.setOnClickListener(this);
        mBinding.audioPlayer.setOnClickListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void  initAudio() {
        // 获得缓冲区字节大小
        recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLING_RATE_IN_HZ,
                CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
//         bufferSizeInBytes = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING);
//        audioRecord = new AudioRecord(AUDIO_INPUT, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING, bufferSizeInBytes);
//        this.fileName = fileName;
//        status = AudioStatus.STATUS_READY;

        AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();

        AudioFormat audioFormat = new AudioFormat.Builder().setSampleRate(SAMPLING_RATE_IN_HZ)
                .setEncoding(AUDIO_FORMAT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();

        audioTrack = new AudioTrack(audioAttributes, audioFormat, BUFFER_SIZE,
                AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.switchMediaRecorder:
                onSwitch();
                break;
            case R.id.audioRecorder:
                onRecord();
                break;
            case R.id.audioPlayer:
                onPlay();
                break;
        }
    }

    private void onSwitch() {
        mRecording.set(false);
        mPlaying.set(false);
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @SuppressLint("SetTextI18n")
    private void onRecord(){
        if (!mRecording.get()) {
            if (checkPermission()) {
                startRecording();
                mRecording.set(true);
                mBinding.audioRecorder.setText("StopAudioRecorder");
                mBinding.audioPlayer.setEnabled(false);
                mBinding.switchMediaRecorder.setEnabled(false);
            }
        } else {
            stopRecording();
            mRecording.set(false);
            mBinding.audioRecorder.setText("StartAudioRecorder");
            mBinding.audioPlayer.setEnabled(true);
            mBinding.switchMediaRecorder.setEnabled(true);
        }
    }

    @SuppressLint("SetTextI18n")
    private void onPlay(){
        if (!mPlaying.get()) {
            startPlaying();
            mPlaying.set(true);
            mBinding.audioRecorder.setEnabled(false);
            mBinding.switchMediaRecorder.setEnabled(false);
            mBinding.audioPlayer.setText("StopAudioPlayer");
        } else {
            stopPlaying();
            mPlaying.set(false);
            mBinding.audioRecorder.setEnabled(true);
            mBinding.switchMediaRecorder.setEnabled(true);
            mBinding.audioPlayer.setText("StartMediaPlayer");
        }
    }

    private void startRecording() {
        recorder.startRecording();
        mRecording.set(true);

        recordingThread = new Thread(new RecordingRunnable(), "Recording Thread");
        recordingThread.start();
    }

    private void stopRecording() {
        if (null == recorder) {
            return;
        }
        mRecording.set(false);
        recorder.stop();
        recorder.release();
        recorder = null;

        recordingThread = null;
    }

    private void startPlaying() {
        PcmToWav.mergePCMFilesToWAVFile(recordFileName, fileName);
        play(fileName);
    }

    private void stopPlaying() {
        releaseAudioTrack();
    }

    private class RecordingRunnable implements Runnable {

        @Override
        public void run() {
            final File file = new File(recordFileName);
            final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            try (final FileOutputStream outStream = new FileOutputStream(file)) {
                while (mRecording.get()) {
                    int result = recorder.read(buffer, BUFFER_SIZE);
                    if (result < 0) {
                        throw new RuntimeException("Reading of audio buffer failed: " +
                                getBufferReadFailureReason(result));
                    }
                    outStream.write(buffer.array(), 0, BUFFER_SIZE);
                    buffer.clear();
                }
            } catch (IOException e) {
                throw new RuntimeException("Writing of recorded audio failed", e);
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
     * 播放合成后的wav文件
     *
     * @param filePath 文件的绝对路径
     */
    public void play(final String filePath) {
        try {
            audioTrack.play();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Executors.newCachedThreadPool().execute(new Runnable() {

            @Override
            public void run() {
                File file = new File(filePath);
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                byte[] buffer = new byte[BUFFER_SIZE];
                while (fis != null) {
                    try {
                        int readCount = fis.read(buffer);
                        if (readCount == AudioTrack.ERROR_INVALID_OPERATION || readCount == AudioTrack.ERROR_BAD_VALUE) {
                            continue;
                        }
                        if (readCount != 0 && readCount != -1) {
                            audioTrack.write(buffer, 0, readCount);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * 释放audioTrack
     */
    public void releaseAudioTrack(){
        if (audioTrack == null) {
            return;
        }
        if (audioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
            audioTrack.stop();
        }
        audioTrack.release();
        audioTrack = null;
    }
}
