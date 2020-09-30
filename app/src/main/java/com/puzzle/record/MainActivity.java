package com.puzzle.record;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.puzzle.record.databinding.ActivityMainBinding;

import java.io.IOException;
import java.util.Objects;

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
        fileName = Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath() + "/audiorecordtest.3gp";
    }

    @SuppressLint("SetTextI18n")
    private void initView() {
        mBinding.recorder.setText("StartRecorder");
        mBinding.player.setText("StartPlayer");
        mBinding.switchMediaRecorder.setText("SwitchMediaRecorder");
        mBinding.switchAudioRecorder.setText("SwitchAudioRecorder");
        mBinding.player.setEnabled(false);

        mBinding.recorder.setOnClickListener(this);
        mBinding.player.setOnClickListener(this);
        mBinding.switchMediaRecorder.setOnClickListener(this);
        mBinding.switchAudioRecorder.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case  R.id.recorder:
                onRecord();
                break;
            case R.id.player:
                onPlay();
                break;
            case R.id.switchMediaRecorder:
                mRecording.set(false);
                mPlaying.set(false);
                startActivity(new Intent(this, MediaRecorderActivity.class));
                break;
            case R.id.switchAudioRecorder:
                mRecording.set(false);
                mPlaying.set(false);
                startActivity(new Intent(this, AudioRecorderActivity.class));
                break;
        }
     }

     @SuppressLint("SetTextI18n")
     private void onRecord() {
         if (!mRecording.get()) {
             if (checkPermission()) {
                 startRecording();
                 mRecording.set(true);
                 mBinding.recorder.setText("StopRecorder");
                 mBinding.player.setEnabled(false);
                 mBinding.switchMediaRecorder.setEnabled(false);
                 mBinding.switchAudioRecorder.setEnabled(false);
             }
         } else {
             stopRecording();
             mRecording.set(false);
             mBinding.recorder.setText("StartRecorder");
             mBinding.player.setEnabled(true);
             mBinding.switchMediaRecorder.setEnabled(true);
             mBinding.switchAudioRecorder.setEnabled(true);
         }
     }

     @SuppressLint("SetTextI18n")
     private void onPlay() {
        if (!mPlaying.get()) {
            startPlaying();
            mPlaying.set(true);
            mBinding.recorder.setEnabled(false);
            mBinding.switchMediaRecorder.setEnabled(false);
            mBinding.switchAudioRecorder.setEnabled(false);
            mBinding.player.setText("StopPlayer");
        } else {
            stopPlaying();
            mPlaying.set(false);
            mBinding.recorder.setEnabled(true);
            mBinding.switchMediaRecorder.setEnabled(true);
            mBinding.switchAudioRecorder.setEnabled(true);
            mBinding.player.setText("StartPlayer");
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