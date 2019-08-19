package com.winllc.acme.server.util;

import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NonceUtil {

    public static List<String> usedNonces = new ArrayList<>();
    public static List<String> unUsedNonces = new ArrayList<>();

    public static String generateNonce() {
        String dateTimeString = Long.toString(new Date().getTime());
        byte[] nonceByte = dateTimeString.getBytes();
        String nonce = Base64.encodeBase64String(nonceByte);
        unUsedNonces.add(nonce);
        return nonce;
    }

    public static void markNonceUsed(String nonce){
        usedNonces.add(nonce);
    }

    public static boolean checkNonceUsed(String nonce){
        return usedNonces.contains(nonce);
    }

}
