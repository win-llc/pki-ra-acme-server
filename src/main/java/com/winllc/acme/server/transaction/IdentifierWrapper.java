package com.winllc.acme.server.transaction;

import com.winllc.acme.common.constants.ProblemType;
import com.winllc.acme.common.model.acme.Identifier;
import com.winllc.acme.common.model.acme.ProblemDetails;
import com.winllc.acme.server.exceptions.AcmeServerException;

class IdentifierWrapper {
    Identifier identifier;
    TransactionContext transactionContext;

    IdentifierWrapper(Identifier identifier, TransactionContext transactionContext) {
        this.identifier = identifier;
        this.transactionContext = transactionContext;
    }

    boolean canIssue() throws AcmeServerException {
        boolean canIssue = this.transactionContext.getCa()
                .canIssueToIdentifier(identifier, transactionContext.getAccountData(),
                transactionContext.getDirectoryData());
        if(!canIssue){
            ProblemDetails temp = new ProblemDetails(ProblemType.UNSUPPORTED_IDENTIFIER);
            temp.setDetail("CA can't issue for: " + identifier);
            throw new AcmeServerException(temp);
        }else{
            return true;
        }
    }
}