package com.winllc.acme.server.transaction;

import com.winllc.acme.common.contants.ProblemType;
import com.winllc.acme.common.model.acme.Identifier;
import com.winllc.acme.common.model.acme.ProblemDetails;
import com.winllc.acme.common.model.data.AuthorizationData;
import com.winllc.acme.server.exceptions.AcmeServerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PreAuthzTransaction extends AuthorizationTransaction {

    private final Logger log = LogManager.getLogger(PreAuthzTransaction.class);

    AuthorizationDataWrapper wrapper;

    PreAuthzTransaction(TransactionContext transactionContext) {
        super(transactionContext);
    }

    public AuthorizationData getData(){
        return wrapper.authorizationData;
    }

    @Override
    public ChallengeDataWrapper retrieveChallengeData(String challengeId) {
        return this.wrapper.challengeDataWrappers.stream()
                .filter(w -> w.challengeData.getId().contentEquals(challengeId))
                .findFirst().get();
    }

    @Override
    public void markChallengeComplete(String challengeId) {
        ChallengeDataWrapper challengeDataWrapper = retrieveChallengeData(challengeId);
        challengeDataWrapper.markValid();
    }

    @Override
    public AuthorizationData retrieveAuthorizationData(String id) {
        return this.wrapper.authorizationData;
    }

    //Pre-approved means this identifier was marked to be explicitly allowed for the requesting account
    public void start(Identifier identifier, boolean preApproved) throws Exception {
        IdentifierWrapper identifierWrapper = new IdentifierWrapper(identifier, transactionContext);

        if(identifierWrapper.canIssue()) {

            this.wrapper = new AuthorizationDataWrapper(identifier, transactionContext);

            if (this.wrapper.challengeDataWrappers != null && preApproved) {
                log.info("Identifier is pre-approved, mark challenges valid");
                for (ChallengeDataWrapper challengeDataWrapper : this.wrapper.challengeDataWrappers) {
                    challengeDataWrapper.markValid();
                }
            }
        }else{
            //todo verify this is the right error type
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.REJECTED_IDENTIFIER);
            problemDetails.setDetail("Will not issue to: "+identifier.getValue());
            throw new AcmeServerException(problemDetails);
        }
    }
}
