package com.winllc.acme.server.service.acme;

import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.util.NonceUtil;
import com.winllc.acme.server.util.SecurityValidatorUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

public abstract class BaseService {

    protected ResponseEntity.BodyBuilder buildBaseResponseEntity(int status, DirectoryData directoryData){
        return ResponseEntity.status(status)
                .header("Replay-Nonce", NonceUtil.generateNonce())
                .header("Link", directoryData.buildLinkUrl());
    }

    protected ResponseEntity.BodyBuilder buildBaseResponseEntity(int status, DirectoryData directoryData, HttpHeaders headers){
        headers.add("Replay-Nonce", NonceUtil.generateNonce());
        headers.add("Link", directoryData.buildLinkUrl());
        return ResponseEntity.status(status)
                .headers(headers);
    }
}
