package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.data.AuthorizationData;
import com.winllc.acme.server.model.data.ChallengeData;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ChallengePersistence implements DataPersistence<ChallengeData> {

    private Map<String, ChallengeData> challengeDataMap = new HashMap<>();

    @Override
    public Optional<ChallengeData> getById(String id) {
        //TODO
        return Optional.of(challengeDataMap.get(id));
    }

    @Override
    public ChallengeData save(ChallengeData data) {
        //TODO
        challengeDataMap.put(data.getId(), data);
        return data;
    }

    public List<ChallengeData> getAllChallengesForAuthorization(AuthorizationData authorizationData){
        //TODO
        return null;
    }
}
