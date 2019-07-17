package com.winllc.acme.server.contants;

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
}
