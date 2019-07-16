package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.data.AuthorizationData;
import com.winllc.acme.server.model.data.ChallengeData;

import java.util.List;
import java.util.Optional;

public class ChallengePersistence implements DataPersistence<ChallengeData> {

    @Override
    public Optional<ChallengeData> getById(String id) {
        //TODO
        return Optional.empty();
    }

    @Override
    public ChallengeData save(ChallengeData data) {
        //TODO
        return null;
    }

    public List<ChallengeData> getAllChallengesForAuthorization(AuthorizationData authorizationData){
        //TODO
        return null;
    }
}
