package com.winllc.acme.server.transaction;

import com.winllc.acme.common.contants.ChallengeType;
import com.winllc.acme.common.model.data.AuthorizationData;
import com.winllc.acme.server.challenge.HttpChallengeRunner;

public abstract class AuthorizationTransaction extends AbstractTransaction {
    AuthorizationTransaction(TransactionContext transactionContext) {
        super(transactionContext);
    }

    public abstract ChallengeDataWrapper retrieveChallengeData(String challengeId);

    public void attemptChallenge(String challengeId){
        ChallengeDataWrapper challengeDataWrapper = retrieveChallengeData(challengeId);
        ChallengeType challengeType = ChallengeType.getValue(challengeDataWrapper.challengeData.getObject().getType());
        switch (challengeType){
            case HTTP:
                this.transactionContext.getTaskExecutor()
                        .execute(new HttpChallengeRunner.VerificationRunner(challengeId, this));
                break;
            case DNS:
                //todo
                break;
        }
    }

    //todo generic
    public abstract void markChallengeComplete(String challengeId);

    public abstract AuthorizationData retrieveAuthorizationData(String id);
}
