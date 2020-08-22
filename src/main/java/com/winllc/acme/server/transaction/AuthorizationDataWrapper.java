package com.winllc.acme.server.transaction;

import com.winllc.acme.common.contants.ChallengeType;
import com.winllc.acme.common.contants.StatusType;
import com.winllc.acme.common.model.acme.Authorization;
import com.winllc.acme.common.model.acme.Challenge;
import com.winllc.acme.common.model.acme.Identifier;
import com.winllc.acme.common.model.data.AuthorizationData;
import com.winllc.acme.common.model.data.ChallengeData;

import java.util.ArrayList;
import java.util.List;

/*
                   pending --------------------+
                      |                        |
    Challenge failure |                        |
           or         |                        |
          Error       |  Challenge valid       |
            +---------+---------+              |
            |                   |              |
            V                   V              |
         invalid              valid            |
                                |              |
                                |              |
                                |              |
                 +--------------+--------------+
                 |              |              |
                 |              |              |
          Server |       Client |   Time after |
          revoke |   deactivate |    "expires" |
                 V              V              V
              revoked      deactivated      expired
 */
class AuthorizationDataWrapper extends DataWrapper<AuthorizationData> {
    boolean isPreAuthz = false;
    OrderDataWrapper orderDataWrapper;
    AuthorizationData authorizationData;
    List<ChallengeDataWrapper> challengeDataWrappers;

    AuthorizationDataWrapper(OrderDataWrapper orderDataWrapper,
                             Identifier identifier, TransactionContext transactionContext) throws Exception {
        super(transactionContext);
        this.orderDataWrapper = orderDataWrapper;
        init(identifier);
    }

    AuthorizationDataWrapper(OrderDataWrapper orderDataWrapper,
                             AuthorizationData authorizationData, TransactionContext transactionContext){
        super(transactionContext);
        initExisting(orderDataWrapper, authorizationData);
    }

    AuthorizationDataWrapper(Identifier identifier, TransactionContext transactionContext) throws Exception {
        super(transactionContext);
        init(identifier);
        this.authorizationData.setPreAuthz(true);
        persist();
    }

    void markValid(){
        if(getStatus() == StatusType.PENDING){
            updateStatus(StatusType.VALID);
            orderDataWrapper.authzCompleted();
        }
    }

    void markInvalid(){
        if(getStatus() == StatusType.PENDING){
            updateStatus(StatusType.INVALID);
        }
    }

    void markRevoked(){
        if(getStatus() == StatusType.VALID){
            updateStatus(StatusType.REVOKED);
        }
    }

    void markDeactivated(){
        if(getStatus() == StatusType.VALID){
            updateStatus(StatusType.DEACTIVATED);
        }
    }

    void markExpired(){
        if(getStatus() == StatusType.VALID || getStatus() == StatusType.PENDING){
            updateStatus(StatusType.EXPIRED);
        }
    }

    void init(Identifier identifier) throws Exception {
        IdentifierWrapper identifierWrapper = new IdentifierWrapper(identifier, super.transactionContext);
        if (identifierWrapper.canIssue()) {
            this.challengeDataWrappers = new ArrayList<>();

            Authorization authorization = new Authorization();
            authorization.markPending();
            authorization.setIdentifier(identifier);

            this.authorizationData = new AuthorizationData(authorization, super.transactionContext.getDirectoryData().getName());

            List<ChallengeType> identifierChallengeRequirements =
                    super.transactionContext.getCa().getIdentifierChallengeRequirements(identifier,
                            super.transactionContext.getAccountData(), super.transactionContext.getDirectoryData());

            if (identifierChallengeRequirements != null) {
                for (ChallengeType challengeType : identifierChallengeRequirements) {
                    ChallengeDataWrapper challengeDataWrapper = new ChallengeDataWrapper(this, challengeType, super.transactionContext);
                    this.authorizationData.getObject().addChallenge(challengeDataWrapper.challengeData.getObject());
                    this.challengeDataWrappers.add(challengeDataWrapper);
                }
            }

            if(this.challengeDataWrappers.size() == 0){
                markValid();
                orderDataWrapper.markReady();

                ChallengeDataWrapper challengeDataWrapper =
                        new ChallengeDataWrapper(this, ChallengeType.HTTP, this.transactionContext);
                challengeDataWrapper.updateStatus(StatusType.VALID);

                this.challengeDataWrappers.add(challengeDataWrapper);

            }else{
                persist();
            }

        } else {
            throw new Exception("Cannot issue to: " + identifier.getValue());
        }
    }

    void initExisting(OrderDataWrapper orderDataWrapper, AuthorizationData authorizationData){
        this.orderDataWrapper = orderDataWrapper;
        this.authorizationData = authorizationData;

        this.challengeDataWrappers = new ArrayList<>();

        List<ChallengeData> challenges = super.transactionContext.getChallengePersistence()
                .findAllByAuthorizationIdEquals(this.authorizationData.getId());
        for(ChallengeData challengeData : challenges){
            this.challengeDataWrappers.add(new ChallengeDataWrapper(this, challengeData, super.transactionContext));
        }
    }

    void challengeCompleted(){
        int total = challengeDataWrappers.size();
        int complete = 0;
        for(ChallengeDataWrapper wrapper : challengeDataWrappers){
            if(wrapper.getStatus() == StatusType.VALID) complete++;
        }

        if(complete > 0){
            markValid();
            if(!isPreAuthz) {
                orderDataWrapper.authzCompleted();
            }
        }else{
            System.out.println("All challenges not complete");
        }
    }

    public AuthorizationData getData(){
        return this.authorizationData;
    }

    void persist() {
        this.authorizationData.addTransactionId(super.transactionContext.transactionId);
        this.authorizationData = super.transactionContext.getAuthorizationPersistence().save(this.authorizationData);
    }
}
