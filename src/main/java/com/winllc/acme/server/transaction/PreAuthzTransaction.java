package com.winllc.acme.server.transaction;

import com.winllc.acme.common.model.acme.Authorization;
import com.winllc.acme.common.model.acme.Identifier;
import com.winllc.acme.common.model.data.AuthorizationData;
import com.winllc.acme.common.model.data.ChallengeData;

public class PreAuthzTransaction extends AuthorizationTransaction {

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

    public void start(Identifier identifier) throws Exception {
        this.wrapper = new AuthorizationDataWrapper(identifier, transactionContext);
    }
}
