package com.winllc.acme.server.service;

import com.winllc.acme.common.contants.ProblemType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.common.model.AcmeURL;
import com.winllc.acme.common.model.acme.ProblemDetails;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.server.service.acme.BaseService;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import com.winllc.acme.server.service.internal.ExternalAccountProviderService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler extends BaseService {

    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);

    @Autowired
    private DirectoryDataService directoryDataService;

    @ExceptionHandler(value = { Exception.class })
    protected ResponseEntity<?> handleException(Exception ex, HttpServletRequest request) throws Exception {
        AcmeURL acmeURL = new AcmeURL(request);
        DirectoryData directoryData = directoryDataService.findByName(acmeURL.getDirectoryIdentifier());

        ProblemDetails problemDetails;
        if(ex instanceof AcmeServerException){
            problemDetails = new ProblemDetails(((AcmeServerException) ex).getProblemType());
        }else{
            problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
        }

        log.error("Could not process request to: "+request.getRequestURI(), ex);

        return buildErrorResponseEntity(problemDetails, directoryData);
    }
}
