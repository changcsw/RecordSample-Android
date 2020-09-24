package com.puzzle.record;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.concurrent.atomic.AtomicBoolean;

public class BaseActivity extends AppCompatActivity {
    protected static final String TAG = "demo";

    protected static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    protected static String fileName = null;

    protected AtomicBoolean mRecording = new AtomicBoolean(false);
    protected AtomicBoolean mPlaying = new AtomicBoolean(false);

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    protected String [] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    protected boolean checkPermission(){
        if (!permissionToRecordAccepted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        }
        return permissionToRecordAccepted;
    }
}
