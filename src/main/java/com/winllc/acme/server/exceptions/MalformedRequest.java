package com.winllc.acme.server.exceptions;

import com.winllc.acme.common.contants.ProblemType;

public class MalformedRequest extends AcmeServerException {
    public MalformedRequest() {
        super(ProblemType.MALFORMED);
    }
}
