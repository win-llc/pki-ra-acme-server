package com.winllc.acme.server.contants;

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
