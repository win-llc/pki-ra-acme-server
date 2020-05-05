package com.winllc.acme.server.model.acme;

import com.winllc.acme.server.contants.IdentifierType;

public class Identifier {
    //required
    private String type;
    //required
    private String value;

    public Identifier(IdentifierType type, String value) {
        this.type = type.toString();
        this.value = value;
    }

    public Identifier() {
    }

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

    @Override
    public String toString() {
        return "Identifier{" +
                "type='" + type + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}