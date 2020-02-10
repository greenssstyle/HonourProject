package com.example.thinkpad.voiceassistant.params;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import com.baidu.aip.asrwakeup3.core.util.FileUtil;
import com.baidu.aip.asrwakeup3.core.util.MyLogger;
import com.baidu.speech.asr.SpeechConstant;
import com.example.thinkpad.voiceassistant.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CommonParameter {

    protected String samplePath;

    protected ArrayList<String> stringParams = new ArrayList<String>();
    protected ArrayList<String> intParams = new ArrayList<String>();
    protected ArrayList<String> boolParams = new ArrayList<String>();

    private static final String TAG = "CommonParameter";


    public CommonParameter() {

        stringParams.addAll(Arrays.asList(
                SpeechConstant.VAD,
                SpeechConstant.IN_FILE
        ));
        intParams.addAll(Arrays.asList(
                SpeechConstant.PID,
                SpeechConstant.VAD_ENDPOINT_TIMEOUT
        ));
        boolParams.addAll(Arrays.asList(
                SpeechConstant.ACCEPT_AUDIO_DATA,
                SpeechConstant.ACCEPT_AUDIO_VOLUME
        ));
    }


    public void initSamplePath(Context context) {
        String sampleDir = "baiduASR";
        samplePath = Environment.getExternalStorageDirectory().toString() + "/" + sampleDir;
        if (!FileUtil.makeDir(samplePath)) {
            samplePath = context.getExternalFilesDir(sampleDir).getAbsolutePath();
            if (!FileUtil.makeDir(samplePath)) {
                throw new RuntimeException("Failed to build temporary directory:" + samplePath);
            }
        }
    }

    public Map<String, Object> fetch(SharedPreferences sp) {
        Map<String, Object> map = new HashMap<String, Object>();
        parseParamArr(sp, map);

        if (sp.getBoolean("_tips_sound", false)) {
            map.put(SpeechConstant.SOUND_START, R.raw.bdspeech_recognition_start);
            map.put(SpeechConstant.SOUND_END, R.raw.bdspeech_speech_end);
            map.put(SpeechConstant.SOUND_SUCCESS, R.raw.bdspeech_recognition_success);
            map.put(SpeechConstant.SOUND_ERROR, R.raw.bdspeech_recognition_error);
            map.put(SpeechConstant.SOUND_CANCEL, R.raw.bdspeech_recognition_cancel);
        }

        if (sp.getBoolean("_outfile", false)) {
            map.put(SpeechConstant.ACCEPT_AUDIO_DATA, true);
            map.put(SpeechConstant.OUT_FILE, samplePath + "/outfile.pcm");
            MyLogger.info(TAG, "Save recorded file at：" + samplePath + "/outfile.pcm");
        }

        return map;
    }

    private void parseParamArr(SharedPreferences sp, Map<String, Object> map) {

        for (String name : stringParams) {
            if (sp.contains(name)) {
                String tmp = sp.getString(name, "").replaceAll(",.*", "").trim();
                if (null != tmp && !"".equals(tmp)) {
                    map.put(name, tmp);
                }
            }
        }

        for (String name : intParams) {
            if (sp.contains(name)) {
                String tmp = sp.getString(name, "").replaceAll(",.*", "").trim();
                if (null != tmp && !"".equals(tmp)) {
                    map.put(name, Integer.parseInt(tmp));
                }
            }
        }

        for (String name : boolParams) {
            if (sp.contains(name)) {
                boolean res = sp.getBoolean(name, false);
                if (res || name.equals(SpeechConstant.ACCEPT_AUDIO_VOLUME)) {
                    map.put(name, res);
                }
            }
        }
    }
}
