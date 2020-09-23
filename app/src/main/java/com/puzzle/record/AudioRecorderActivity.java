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

    private void onRecord(){

    }

    private void onPlay(){

    }
}
