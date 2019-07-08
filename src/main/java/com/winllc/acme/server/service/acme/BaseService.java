package com.winllc.acme.server.service.acme;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.util.AppUtil;
import org.springframework.http.ResponseEntity;

public abstract class BaseService {

    protected ResponseEntity.BodyBuilder buildBaseResponseEntity(int status){
        return ResponseEntity.status(status)
                .header("Replay-Nonce", AppUtil.generateNonce())
                .header("Link", Application.directoryData.buildLinkUrl());
    }
}
