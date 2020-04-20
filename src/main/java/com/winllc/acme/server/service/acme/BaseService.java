package com.winllc.acme.server.service.acme;

import com.winllc.acme.server.model.acme.ProblemDetails;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.util.NonceUtil;
import com.winllc.acme.server.util.SecurityValidatorUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public abstract class BaseService {

    protected ResponseEntity buildErrorResponseEntity(ProblemDetails problemDetails, DirectoryData directoryData){
        return ResponseEntity.status(problemDetails.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .header("Replay-Nonce", NonceUtil.generateNonce())
                .header("Link", directoryData.buildLinkUrl())
                .body(problemDetails);
    }

    protected ResponseEntity.BodyBuilder buildBaseResponseEntity(int status, DirectoryData directoryData){
        return ResponseEntity.status(status)
                .header("Replay-Nonce", NonceUtil.generateNonce())
                .header("Link", directoryData.buildLinkUrl());
    }

    protected ResponseEntity.BodyBuilder buildBaseResponseEntityWithRetryAfter(int status, DirectoryData directoryData, int waitInSeconds){
        ResponseEntity.BodyBuilder base = buildBaseResponseEntity(status, directoryData);
        return base.header("Retry-After", ""+waitInSeconds);
    }

    protected ResponseEntity.BodyBuilder buildBaseResponseEntity(int status, DirectoryData directoryData, HttpHeaders headers){
        headers.add("Replay-Nonce", NonceUtil.generateNonce());
        headers.add("Link", directoryData.buildLinkUrl());
        return ResponseEntity.status(status)
                .headers(headers);
    }
}
