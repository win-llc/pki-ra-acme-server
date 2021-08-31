package com.winllc.acme.server.transaction;

import com.winllc.acme.common.constants.ChallengeType;
import com.winllc.acme.common.constants.StatusType;
import com.winllc.acme.common.model.acme.Authorization;
import com.winllc.acme.common.model.acme.Identifier;
import com.winllc.acme.common.model.data.AuthorizationData;
import com.winllc.acme.common.model.data.ChallengeData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private static final Logger log = LogManager.getLogger(AuthorizationDataWrapper.class);

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
        this.orderDataWrapper = orderDataWrapper;
        this.authorizationData = authorizationData;
        reloadChildren();
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
            if(orderDataWrapper != null) {
                this.authorizationData.setOrderId(orderDataWrapper.getData().getId());
            }

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

    @Override
    void reloadChildren() {
        this.challengeDataWrappers = new ArrayList<>();
        List<ChallengeData> challenges = super.transactionContext.getChallengePersistence()
                .findAllByAuthorizationIdEquals(this.authorizationData.getId());

        if(challenges != null) {
            for (ChallengeData challengeData : challenges) {
                this.challengeDataWrappers.add(new ChallengeDataWrapper(this, challengeData, super.transactionContext));
            }
        }else{
            log.info("No challenges for: "+authorizationData.getId());
        }
    }


    void challengeCompleted(){
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
            log.info("All challenges not complete");
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
