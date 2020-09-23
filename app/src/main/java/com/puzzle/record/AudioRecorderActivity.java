package com.puzzle.record;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.puzzle.record.databinding.ActivityAudioBinding;


public class AudioRecorderActivity extends BaseActivity implements View.OnClickListener {

    ActivityAudioBinding mBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityAudioBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        initView();
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
                // startRecording();
                mRecording.set(true);
                mBinding.audioRecorder.setText("StopAudioRecorder");
                mBinding.audioPlayer.setEnabled(false);
                mBinding.switchMediaRecorder.setEnabled(false);
            }
        } else {
            // stopRecording();
            mRecording.set(false);
            mBinding.audioRecorder.setText("StartAudioRecorder");
            mBinding.audioPlayer.setEnabled(true);
            mBinding.switchMediaRecorder.setEnabled(true);
        }
    }

    @SuppressLint("SetTextI18n")
    private void onPlay(){
        if (!mPlaying.get()) {
            // startPlaying();
            mPlaying.set(true);
            mBinding.audioRecorder.setEnabled(false);
            mBinding.switchMediaRecorder.setEnabled(false);
            mBinding.audioPlayer.setText("StopAudioPlayer");
        } else {
            // stopPlaying();
            mPlaying.set(false);
            mBinding.audioRecorder.setEnabled(true);
            mBinding.switchMediaRecorder.setEnabled(true);
            mBinding.audioPlayer.setText("StartMediaPlayer");
        }
    }
}
