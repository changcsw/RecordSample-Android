package com.puzzle.record;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;

import com.puzzle.record.databinding.ActivityMainBinding;
import com.puzzle.record.utils.PcmToWav;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    private ActivityMainBinding mBinding;

    private byte[] mAudioData;
    private String mFilePath;
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
        AudioManager.getInstance().init(this);
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
        AudioHelper.getInstance().startRecord(audioData -> {
            mAudioData = audioData;

            // 将音频数据转换为文件路径，可以使用AudioManager中的 startPlay来播放，如果用AudioHelper中的startPlay播放则不需要下面的转换过程
            String filePath = getExternalCacheDir() + "/record";
            String fileName = SystemClock.uptimeMillis() + ".pcm";
            String recordFileName = filePath + File.separator + fileName;
            String finalFileName = recordFileName.replace(".pcm", ".mp3");

            createFile(mAudioData, filePath, fileName);

            PcmToWav.mergePCMFilesToWAVFile(recordFileName, finalFileName);

            mFilePath = finalFileName;

        });
//        AudioManager.getInstance().startRecord(filePath -> {
//            mFilePath = filePath;
//
//            // 使用AudioHelper中的startPlay来播放，才需要转换，使用 AudioManager中的startPlay播放，则不需要下面的转换
//            try {
//                mAudioData = toByteArray(filePath);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
    }

    private void stopRecording() {
        AudioHelper.getInstance().stopRecord();
//        AudioManager.getInstance().stopRecord();
    }

    private void startPlaying() {
        AudioHelper.getInstance().startPlay(mAudioData, new AudioHelper.OnPlayListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onPlayFinish() {
                runOnUiThread(() -> {
                    mPlaying.set(false);
                    mBinding.recorder.setEnabled(true);
                    mBinding.switchMediaRecorder.setEnabled(true);
                    mBinding.switchAudioRecorder.setEnabled(true);
                    mBinding.player.setText("StartPlayer");
                });
            }
        });
//        File file = new File(mFilePath);
//        Log.d(TAG, "mFilePath=" + mFilePath + ", isExist=" + file.isFile());
//        AudioManager.getInstance().startPlay(mFilePath);
    }

    private void stopPlaying() {
        AudioHelper.getInstance().stopPlay();
//        AudioManager.getInstance().stopPlay();
    }

    public static void createFile(byte[] qrData, String filePath,String fileName) {
        OutputStream os = null;
        try {

            File dir = new File(filePath);
            if(!dir.exists()&&dir.isDirectory()){//判断文件目录是否存在
                dir.mkdirs();
            }

            os = new FileOutputStream(filePath + File.separator + fileName);
            os.write(qrData, 0, qrData.length);
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public static void getFile(byte[] bfile, String filePath,String fileName) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        File file = null;
        try {
            File dir = new File(filePath);
            if(!dir.exists()&&dir.isDirectory()){//判断文件目录是否存在
                dir.mkdirs();
            }
            file = new File(filePath + File.separator + fileName);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(bfile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public static byte[] toByteArray(String filename) throws IOException {

        File f = new File(filename);
        if (!f.exists()) {
            throw new FileNotFoundException(filename);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream((int) f.length());
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(f));
            int buf_size = 1024;
            byte[] buffer = new byte[buf_size];
            int len = 0;
            while (-1 != (len = in.read(buffer, 0, buf_size))) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            bos.close();
        }
    }

}