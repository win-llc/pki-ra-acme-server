package com.winllc.acme.server.model.acme;

public class Identifier {
    //required
    private String type;
    //required
    private String value;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}