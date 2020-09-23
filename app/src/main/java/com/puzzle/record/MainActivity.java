package com.puzzle.record;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;

import com.puzzle.record.databinding.ActivityMainBinding;

import java.io.IOException;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    private MediaRecorder recorder = null;
    private MediaPlayer player = null;

    private ActivityMainBinding mBinding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        initView();
    }

    @SuppressLint("SetTextI18n")
    private void initView() {
        mBinding.mediaRecorder.setText("StartMediaRecorder");
        mBinding.mediaPlayer.setText("StartMediaPlayer");
        mBinding.switchAudioRecorder.setText("SwitchAudioRecorder");
        mBinding.mediaPlayer.setEnabled(false);

        mBinding.mediaRecorder.setOnClickListener(this);
        mBinding.mediaPlayer.setOnClickListener(this);
        mBinding.switchAudioRecorder.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case  R.id.mediaRecorder:
                onRecord();
                break;
            case R.id.mediaPlayer:
                onPlay();
                break;
            case R.id.switchAudioRecorder:
                mRecording.set(false);
                mPlaying.set(false);
                Intent intent = new Intent(this, AudioRecorderActivity.class);
                startActivity(intent);
                break;
        }
     }

     @SuppressLint("SetTextI18n")
     private void onRecord() {
         if (!mRecording.get()) {
             if (checkPermission()) {
                 startRecording();
                 mRecording.set(true);
                 mBinding.mediaRecorder.setText("StopMediaRecorder");
                 mBinding.mediaPlayer.setEnabled(false);
                 mBinding.switchAudioRecorder.setEnabled(false);
             }
         } else {
             stopRecording();
             mRecording.set(false);
             mBinding.mediaRecorder.setText("StartMediaRecorder");
             mBinding.mediaPlayer.setEnabled(true);
             mBinding.switchAudioRecorder.setEnabled(true);
         }
     }

     @SuppressLint("SetTextI18n")
     private void onPlay() {
        if (!mPlaying.get()) {
            startPlaying();
            mPlaying.set(true);
            mBinding.mediaRecorder.setEnabled(false);
            mBinding.switchAudioRecorder.setEnabled(false);
            mBinding.mediaPlayer.setText("StopMediaPlayer");
        } else {
            stopPlaying();
            mPlaying.set(false);
            mBinding.mediaRecorder.setEnabled(true);
            mBinding.switchAudioRecorder.setEnabled(true);
            mBinding.mediaPlayer.setText("StartMediaPlayer");
        }
     }

     MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
         @Override
         public void onCompletion(MediaPlayer mp) {
             onPlay();
         }
     };

    private void startPlaying() {
        player = new MediaPlayer();
        try {
            player.setDataSource(fileName);
            player.setOnCompletionListener(onCompletionListener);
            player.prepare();
            player.start();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        player.release();
        player = null;
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        recorder.start();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }

        if (player != null) {
            player.release();
            player = null;
        }
    }

}