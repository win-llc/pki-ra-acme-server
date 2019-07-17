package com.winllc.acme.server.service;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.model.AcmeURL;
import com.winllc.acme.server.model.acme.ProblemDetails;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.service.acme.BaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler extends BaseService {

    @ExceptionHandler(value = { Exception.class })
    protected ResponseEntity<?> handleException(Exception ex, HttpServletRequest request){
        AcmeURL acmeURL = new AcmeURL(request);
        DirectoryData directoryData = Application.directoryDataMap.get(acmeURL.getDirectoryIdentifier());

        ProblemDetails problemDetails;
        if(ex instanceof AcmeServerException){
            problemDetails = new ProblemDetails(((AcmeServerException) ex).getProblemType());
        }else{
            problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
        }

        return buildBaseResponseEntity(500, directoryData)
                .body(problemDetails);
    }
}
