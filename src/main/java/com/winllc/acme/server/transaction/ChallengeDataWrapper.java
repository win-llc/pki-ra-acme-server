package com.winllc.acme.server.transaction;

import com.winllc.acme.common.contants.ChallengeType;
import com.winllc.acme.common.contants.StatusType;
import com.winllc.acme.common.model.acme.Challenge;
import com.winllc.acme.common.model.data.ChallengeData;
import com.winllc.acme.server.Application;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Base64;

/*
         pending
        |
        | Receive
        | response
        V
    processing <-+
        |   |    | Server retry or
        |   |    | client retry request
        |   +----+
        |
        |
Successful  |   Failed
validation  |   validation
+---------+---------+
|                   |
V                   V
valid              invalid
 */
public class ChallengeDataWrapper extends DataWrapper<ChallengeData> {
    AuthorizationDataWrapper authorizationData;
    ChallengeData challengeData;

    ChallengeDataWrapper(AuthorizationDataWrapper authorizationData,
                         ChallengeType challengeType, TransactionContext transactionContext) {
        super(transactionContext);
        this.authorizationData = authorizationData;
        Challenge challenge = new Challenge();
        challenge.markPending();

        String token = RandomStringUtils.random(50);

        Base64.Encoder urlEncoder = java.util.Base64.getUrlEncoder().withoutPadding();
        String encoded = urlEncoder.encodeToString(token.getBytes());
        challenge.setToken(encoded);
        challenge.setStatus(StatusType.PENDING.toString());

        this.challengeData = new ChallengeData(challenge, transactionContext.getDirectoryData().getName());
        this.challengeData.setAuthorizationId(authorizationData.authorizationData.getId());

        this.challengeData.getObject().setUrl(this.challengeData.buildUrl(Application.baseURL));
        this.challengeData.getObject().setType(challengeType.toString());

        persist();
    }

    ChallengeDataWrapper(AuthorizationDataWrapper authorizationDataWrapper,
                         ChallengeData challengeData, TransactionContext transactionContext){
        super(transactionContext);
        this.challengeData = challengeData;
        this.authorizationData = authorizationDataWrapper;
    }

    public ChallengeData getData(){
        return this.challengeData;
    }

    void markProcessing(){
        if(getStatus() == StatusType.PROCESSING || getStatus() == StatusType.PENDING){
            updateStatus(StatusType.PROCESSING);
        }
    }

    void markValid(){
        if(getStatus() == StatusType.PROCESSING) {
            updateStatus(StatusType.VALID);
            this.authorizationData.challengeCompleted();
        }
    }

    void markInvalid(){
        if(getStatus() == StatusType.PROCESSING) {
            updateStatus(StatusType.INVALID);
        }
    }

    void persist() {
        this.challengeData.addTransactionId(super.transactionContext.transactionId);
        this.challengeData = super.transactionContext.getChallengePersistence().save(this.challengeData);
    }
}