package com.example.thinkpad.voiceassistant;

public class ContactInfo {

    private String name;
    private String number;


    public ContactInfo(String name, String number) {
        this.name = name;
        this.number = number;
    }


    public String getName() {
        return name;
    }

    public String getNumber() {
        return number;
    }
}

