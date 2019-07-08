package com.winllc.acme.server.service.acme;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.util.AppUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NonceService {

    @RequestMapping(value = "new-nonce", method = RequestMethod.HEAD)
    public ResponseEntity<?> newNonceHead(){
        return ResponseEntity.ok()
                .headers(generateHeaders())
                .build();
    }

    @RequestMapping(value = "new-nonce", method = RequestMethod.GET)
    public ResponseEntity<?> newNonceGet(){
        return ResponseEntity.noContent()
                .headers(generateHeaders())
                .build();
    }

    private HttpHeaders generateHeaders(){
        String nonce = AppUtil.generateNonce();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Replay-Nonce", nonce);
        headers.add("Cache-Control", "no-store");
        //TODO
        headers.add("Link", "TODO");
        return headers;
    }

}
