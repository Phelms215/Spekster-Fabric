package com.networkglitch.spekster.datasets;

import java.util.HashMap;
import java.util.UUID;

public class SkinDetails {

    private String name;
    private SkinProperties[] properties;
    private String error;

    public String getError() {
        return error;
    }

    public SkinProperties getProperties() {
        return properties[0];
    }

    public void setError(String errorMessage) {
        error = errorMessage;
    }

    public void setProperties(SkinProperties theseProperties) {
        properties[0] = theseProperties;
    }

    public void setName(String Name) {
        name = Name;
    }
}
