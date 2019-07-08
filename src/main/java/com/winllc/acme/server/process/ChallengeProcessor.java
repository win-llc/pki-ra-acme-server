package com.winllc.acme.server.process;

import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.model.Challenge;
import com.winllc.acme.server.model.data.ChallengeData;
import com.winllc.acme.server.persistence.ChallengePersistence;

public class ChallengeProcessor implements AcmeDataProcessor<ChallengeData> {

    public ChallengeData buildNew(){
        Challenge challenge = new Challenge();
        challenge.setStatus(StatusType.PENDING.toString());

        ChallengeData challengeData = new ChallengeData(challenge);

        //TODO

        return challengeData;
    }
}
