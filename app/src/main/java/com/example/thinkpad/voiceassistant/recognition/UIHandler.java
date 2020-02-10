package com.example.thinkpad.voiceassistant.recognition;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;

import com.baidu.aip.asrwakeup3.core.recog.IStatus;
import com.example.thinkpad.voiceassistant.params.CommonParameter;
import com.example.thinkpad.voiceassistant.params.OnlineParameter;
import com.example.thinkpad.voiceassistant.setting.OnlineSetting;

import java.util.Map;

public abstract class UIHandler extends CommonRecognition implements IStatus {

    private final CommonParameter apiParams;
    private final Class settingActivityClass;

    protected int status;
    protected boolean running = false;


    public UIHandler(int textId) {
        super(textId);
        String className = getClass().getSimpleName();

        if (className.equals("OnlineRecognition")) {
            settingActivityClass = OnlineSetting.class;
            apiParams = new OnlineParameter();
        } else {
            throw new RuntimeException("WRONG ACTIVITY");
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiParams.initSamplePath(this);
    }

    @Override
    protected void initView() {
        super.initView();
        status = STATUS_NONE;

        btn_start.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                switch (status) {
                    case STATUS_NONE:
                        start();
                        status = STATUS_WAITING_READY;
                        updateBtnTextByStatus();
                        tv_Log.setText("");
                        tv_Result.setText("");
                        break;
                    case STATUS_WAITING_READY:
                    case STATUS_READY:
                    case STATUS_SPEAKING:
                    case STATUS_FINISHED:
                    case STATUS_RECOGNITION:
                        stop();
                        status = STATUS_STOPPED;
                        updateBtnTextByStatus();
                        break;
                    case STATUS_LONG_SPEECH_FINISHED:
                    case STATUS_STOPPED:
                        cancel();
                        status = STATUS_NONE;
                        updateBtnTextByStatus();
                        break;
                    default:
                        break;
                }
            }
        });
        if (setting != null && settingActivityClass != null) {
            setting.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    running = true;
                    Intent intent = new Intent(UIHandler.this, settingActivityClass);
                    startActivityForResult(intent, 1);
                }
            });
        }
    }


    private void updateBtnTextByStatus() {
        switch (status) {
            case STATUS_NONE:
                btn_start.setText("Start Recording");
                btn_start.setEnabled(true);
                setting.setEnabled(true);
                break;
            case STATUS_WAITING_READY:
            case STATUS_READY:
            case STATUS_SPEAKING:
            case STATUS_RECOGNITION:
                btn_start.setText("Stop Recording");
                btn_start.setEnabled(true);
                setting.setEnabled(false);
                break;
            case STATUS_LONG_SPEECH_FINISHED:
            case STATUS_STOPPED:
                btn_start.setText("Cancel Recognition");
                btn_start.setEnabled(true);
                setting.setEnabled(false);
                break;
            default:
                break;
        }
    }

    private void returnResults(String str) {
        Intent intent = getIntent();
        intent.putExtra("results", str);
        setResult(RESULT_OK, intent);
        finish();
    }

    protected void handleMsg(Message msg) {
        super.handleMsg(msg);

        switch (msg.what) {
            case STATUS_FINISHED:
                if (msg.arg2 == 1) {
                    String res = msg.obj.toString();
                    res = res.replace("\n", "");
                    tv_Result.setText(res);
                    returnResults(res);
                }
                status = msg.what;
                updateBtnTextByStatus();
                break;
            case STATUS_NONE:
            case STATUS_READY:
            case STATUS_SPEAKING:
            case STATUS_RECOGNITION:
                status = msg.what;
                updateBtnTextByStatus();
                break;
            default:
                break;
        }
    }

    protected Map<String, Object> fetchParams() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        Map<String, Object> params = apiParams.fetch(sp);
        return params;
    }


    protected abstract void start();

    protected abstract void stop();

    protected abstract void cancel();
}
