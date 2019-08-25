package com.winllc.acme.server.model.requestresponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.util.Base64URL;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ExternalAccountBinding {

    @JsonProperty("protected")
    private String protectedProp;
    private String payload;
    private String signature;

    public String getProtectedProp() {
        return protectedProp;
    }

    public void setProtectedProp(String protectedProp) {
        this.protectedProp = protectedProp;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String toJson(){
        Base64URL part1 = new Base64URL(getProtectedProp());
        Base64URL part2 = new Base64URL(getPayload());
        Base64URL part3 = new Base64URL(getSignature());

        Map<String, String> jsonMap = new HashMap<>();
        jsonMap.put("protected", part1.decodeToString());
        jsonMap.put("payload", part2.decodeToString());
        jsonMap.put("signature", part3.toString());

        return new JSONObject(jsonMap).toString();
    }

    @Override
    public String toString() {
        return protectedProp+"."+payload+"."+signature;
    }

}
