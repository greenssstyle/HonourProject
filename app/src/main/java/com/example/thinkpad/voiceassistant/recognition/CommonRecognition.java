package com.example.thinkpad.voiceassistant.recognition;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;

import com.baidu.aip.asrwakeup3.core.inputstream.InFileStream;
import com.baidu.aip.asrwakeup3.core.util.MyLogger;
import com.example.thinkpad.voiceassistant.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public abstract class CommonRecognition extends AppCompatActivity {

    protected TextView tv_Result;
    protected TextView tv_Log;
    protected Button setting;
    protected Button btn_start;

    protected Handler handler;

    protected final int layout;
    private final int textId;


    public CommonRecognition(int textId) {
        this(textId, R.layout.activity_main);
    }

    public CommonRecognition(int textId, int layout) {
        super();
        this.textId = textId;
        this.layout = layout;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InFileStream.setContext(this);
        setContentView(layout);

        initView();
        initPermission();

        handler = new Handler() {

            /*
             * @param msg
             */
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                handleMsg(msg);
            }

        };
        MyLogger.setHandler(handler);
    }

    protected void initView() {
        tv_Result = findViewById(R.id.tv_result);
        tv_Log = findViewById(R.id.tv_log);
        btn_start = findViewById(R.id.btn_start);
        setting = findViewById(R.id.btn_setting);
        String descText = "";
        try {
            InputStream is = getResources().openRawResource(textId);
            byte[] bytes = new byte[is.available()];
            is.read(bytes);
            descText = new String(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        tv_Log.setText(descText);
        tv_Log.append("\n");
    }

    protected void handleMsg(Message msg) {
        if (tv_Log != null && msg.obj != null) {
            tv_Log.append(msg.obj.toString() + "\n");
        }
    }


    /**
     * android 6.0 System Permissions
     */
    private void initPermission() {
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
            }
        }

        String[] tmpList = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    }
}
