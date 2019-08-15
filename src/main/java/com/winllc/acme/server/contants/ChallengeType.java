package com.winllc.acme.server.contants;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//Section 9.7.8
public enum ChallengeType {
    HTTP("http-01"),
    DNS("dns-01");

    private String value;

    ChallengeType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static ChallengeType getValue(String val) {
        for(ChallengeType type : values()){
            if(type.value.contentEquals(val)) return type;
        }
        throw new IllegalArgumentException("Invalid ChallengeType: "+val);
    }
}
