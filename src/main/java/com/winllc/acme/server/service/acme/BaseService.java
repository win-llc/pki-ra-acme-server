package com.winllc.acme.server.service.acme;

import com.winllc.acme.common.model.acme.ProblemDetails;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.util.NonceUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public abstract class BaseService {

    private final NonceUtil nonceUtil;

    protected BaseService(NonceUtil nonceUtil) {
        this.nonceUtil = nonceUtil;
    }

    protected ResponseEntity buildErrorResponseEntity(ProblemDetails problemDetails, DirectoryData directoryData){
        return ResponseEntity.status(problemDetails.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .header("Replay-Nonce", nonceUtil.generateNonce())
                .header("Link", directoryData.buildLinkUrl(Application.baseURL))
                .body(problemDetails);
    }

    protected ResponseEntity.BodyBuilder buildBaseResponseEntity(int status, DirectoryData directoryData){
        return ResponseEntity.status(status)
                .header("Replay-Nonce", nonceUtil.generateNonce())
                .header("Link", directoryData.buildLinkUrl(Application.baseURL));
    }

    protected ResponseEntity.BodyBuilder buildBaseResponseEntityWithRetryAfter(int status, DirectoryData directoryData, int waitInSeconds){
        ResponseEntity.BodyBuilder base = buildBaseResponseEntity(status, directoryData)
                .header("Retry-After", "" + waitInSeconds);
        return base;
    }

    protected ResponseEntity.BodyBuilder buildBaseResponseEntity(int status, DirectoryData directoryData, HttpHeaders headers){
        headers.add("Replay-Nonce", nonceUtil.generateNonce());
        headers.add("Link", directoryData.buildLinkUrl(Application.baseURL));
        return ResponseEntity.status(status)
                .headers(headers);
    }
}
