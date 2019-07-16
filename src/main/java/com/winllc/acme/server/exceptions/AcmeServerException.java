package com.winllc.acme.server.exceptions;

import com.winllc.acme.server.contants.ProblemType;

public class AcmeServerException extends Exception {
    protected ProblemType problemType;
    protected String details;

    public AcmeServerException(ProblemType problemType){
        this.problemType = problemType;
    }

    public AcmeServerException(ProblemType problemType, String details){
        this(problemType);
        this.details = details;
    }

    public ProblemType getProblemType() {
        return problemType;
    }

    public String getDetails() {
        return details;
    }
}
