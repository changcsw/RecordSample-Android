package com.puzzle.record;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.puzzle.record.databinding.ActivityMainBinding;

import java.util.Objects;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    private ActivityMainBinding mBinding;

    private String mFilePath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        initView();
        fileName = Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath() + "/audiorecordtest.3gp";
        AudioManager.getInstance().init(this);
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

    private void startRecording() {
        AudioManager.getInstance().startRecord(filePath -> mFilePath = filePath);
    }

    private void stopRecording() {
        AudioManager.getInstance().stopRecord();
    }

    private void startPlaying() {
        AudioManager.getInstance().startPlay(mFilePath);
    }

    private void stopPlaying() {
        AudioManager.getInstance().stopPlay();
    }

}