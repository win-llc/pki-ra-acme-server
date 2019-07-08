package com.winllc.acme.server.service;

import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.model.ProblemDetails;
import com.winllc.acme.server.service.acme.BaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
public class GlobalExceptionHandler extends BaseService {

    @ExceptionHandler(value = { Exception.class })
    protected ResponseEntity<?> handleException(Exception ex, WebRequest request){

        ProblemDetails problemDetails;
        if(ex instanceof AcmeServerException){
            problemDetails = new ProblemDetails(((AcmeServerException) ex).getProblemType());
        }else{
            problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
        }

        return buildBaseResponseEntity(500)
                .body(problemDetails);
    }
}
