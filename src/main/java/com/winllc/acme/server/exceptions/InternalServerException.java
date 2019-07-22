package com.winllc.acme.server.exceptions;

import com.winllc.acme.server.contants.ProblemType;

public class InternalServerException extends AcmeServerException {
    private InternalServerException(ProblemType problemType) {
        super(problemType);
    }

    public InternalServerException(String details){
        this(ProblemType.SERVER_INTERNAL);
        this.details = details;
    }

    public InternalServerException(String details, Exception subProblem){
        this(details);
        this.addSuppressed(subProblem);
    }
}
