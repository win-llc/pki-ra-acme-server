package com.winllc.acme.server.persistence;

import com.winllc.acme.common.model.data.ChallengeData;

import java.util.List;

public interface ChallengePersistence extends DataPersistence<ChallengeData> {

    List<ChallengeData> findAllByAuthorizationIdEquals(String id);

}
