package com.example.android.tflitecamerademo;

import android.app.Activity;
import android.os.Bundle;

public class CameraActivity extends Activity {
    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(C0195R.layout.activity_camera);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(C0195R.C0197id.container, Camera2BasicFragment.newInstance()).commit();
        }
    }
}
