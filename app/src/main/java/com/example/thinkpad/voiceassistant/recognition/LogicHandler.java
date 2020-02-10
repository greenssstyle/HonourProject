package com.example.thinkpad.voiceassistant.recognition;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.baidu.aip.asrwakeup3.core.mini.AutoCheck;
import com.baidu.aip.asrwakeup3.core.recog.MyRecognizer;
import com.baidu.aip.asrwakeup3.core.recog.listener.IRecogListener;
import com.baidu.aip.asrwakeup3.core.recog.listener.MessageStatusRecogListener;
import com.example.thinkpad.voiceassistant.recognition.UIHandler;
//import com.baidu.aip.asrwakeup3.uiasr.params.OfflineRecogParams;

import java.util.Map;

public abstract class LogicHandler extends UIHandler {

    private static final String TAG = "LogicHandler";
    protected MyRecognizer myRecognizer;
    protected boolean enableOffline;


    public LogicHandler(int textId, boolean enableOffline) {
        super(textId);
        this.enableOffline = enableOffline;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IRecogListener listener = new MessageStatusRecogListener(handler);
        myRecognizer = new MyRecognizer(this, listener);
        if (enableOffline) {}
    }

    @Override
    protected void onDestroy() {
        myRecognizer.release();
        Log.i(TAG, "onDestory");
        super.onDestroy();
    }


    protected void start() {
        final Map<String, Object> params = fetchParams();

        (new AutoCheck(getApplicationContext(), new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 100) {
                    AutoCheck autoCheck = (AutoCheck) msg.obj;
                    synchronized (autoCheck) {
                        String message = autoCheck.obtainErrorMessage();
                        tv_Log.append(message + "\n");
                    }
                }
            }
        }, enableOffline)).checkAsr(params);

        myRecognizer.start(params);
    }

    protected void stop() {
        myRecognizer.stop();
    }

    protected void cancel() {
        myRecognizer.cancel();
    }
}
