package com.winllc.acme.server.contants;

public enum StatusType {
    PENDING("pending"),
    PROCESSING("processing"),
    VALID("valid"),
    INVALID("invalid"),
    REVOKED("revoked"),
    DEACTIVATED("deactivated"),
    EXPIRED("expired"),
    READY("ready");

    private String value;

    StatusType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static StatusType getValue(String val) {
        for(StatusType type : values()){
            if(type.value.contentEquals(val)) return type;
        }
        throw new IllegalArgumentException("Invalid StatusType: "+val);
    }
}
