package com.winllc.acme.server.exceptions;

import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.model.acme.ProblemDetails;

public class AcmeServerException extends Exception {
    protected ProblemType problemType;
    protected ProblemDetails problemDetails;

    public AcmeServerException(ProblemType problemType){
        this.problemType = problemType;
    }

    public AcmeServerException(ProblemType problemType, String details){
        this(problemType);
        ProblemDetails problemDetails = new ProblemDetails(problemType);
        problemDetails.setDetail(details);
        this.problemDetails = problemDetails;
    }

    public AcmeServerException(ProblemDetails problemDetails){
        this(ProblemType.valueToType(problemDetails.getType()));
        this.problemDetails = problemDetails;
    }

    public ProblemType getProblemType() {
        return problemType;
    }

    public ProblemDetails getProblemDetails() {
        return problemDetails;
    }
}
