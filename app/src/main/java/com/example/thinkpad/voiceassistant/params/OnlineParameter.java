package com.example.thinkpad.voiceassistant.params;

import com.baidu.speech.asr.SpeechConstant;

import java.util.Arrays;

public class OnlineParameter extends CommonParameter {

    private static final String TAG = "OnlineParameter";


    public OnlineParameter() {
        super();

        stringParams.addAll(Arrays.asList(
                "_language",
                "_model"
        ));
        intParams.addAll(Arrays.asList(SpeechConstant.PROP));
        boolParams.addAll(Arrays.asList(SpeechConstant.DISABLE_PUNCTUATION));
    }
}
