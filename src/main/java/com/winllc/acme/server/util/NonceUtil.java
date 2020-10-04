package com.winllc.acme.server.util;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class NonceUtil {

    private final CacheManager cacheManager;

    public NonceUtil(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public String generateNonce() {
        String dateTimeString = Long.toString(new Date().getTime());
        byte[] nonceByte = dateTimeString.getBytes();
        String nonce = Base64.encodeBase64String(nonceByte);

        cacheManager.getCache("unusedNonce").put(nonce, nonce);

        return nonce;
    }

    public void markNonceUsed(String nonce){
        cacheManager.getCache("usedNonce").put(nonce, nonce);
    }

    public boolean checkNonceUsed(String nonce){
        Cache.ValueWrapper usedNonce = cacheManager.getCache("usedNonce").get(nonce);
        return usedNonce != null;
    }

}
