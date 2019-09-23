package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.data.AuthorizationData;
import com.winllc.acme.server.model.data.ChallengeData;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ChallengePersistence extends DataPersistence<ChallengeData> {

    List<ChallengeData> findAllByAuthorizationIdEquals(String id);

}
