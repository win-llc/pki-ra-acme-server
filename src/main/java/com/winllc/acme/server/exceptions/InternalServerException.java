package com.winllc.acme.server.exceptions;

import com.winllc.acme.common.constants.ProblemType;

public class InternalServerException extends AcmeServerException {
    private InternalServerException(ProblemType problemType, String details) {
        super(problemType, details);
    }

    public InternalServerException(String details){
        this(ProblemType.SERVER_INTERNAL, details);
    }

    public InternalServerException(String details, Exception subProblem){
        this(details);
        this.addSuppressed(subProblem);
    }
}
