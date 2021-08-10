package com.networkglitch.spekster.datasets;

import java.util.UUID;

public class SkinProperties {

    private String value;
    private String signature;

    public String getTexture() {
        return value;
    }
    public String getSignature() {
        return signature;
    }


    public void setTexture(String text) {
        value = text;
    }
    public void setSignature(String sig) {
        signature = sig;
    }
}
