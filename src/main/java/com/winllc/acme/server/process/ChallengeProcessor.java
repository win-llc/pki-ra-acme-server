package com.winllc.acme.server.process;

import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.exceptions.InternalServerException;
import com.winllc.acme.server.model.acme.Challenge;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.data.AuthorizationData;
import com.winllc.acme.server.model.data.ChallengeData;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.persistence.ChallengePersistence;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;

@Component
public class ChallengeProcessor implements AcmeDataProcessor<ChallengeData> {
    private static final Logger log = LogManager.getLogger(ChallengeProcessor.class);

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

    @Autowired
    private ChallengePersistence challengePersistence;
    @Autowired
    private AuthorizationProcessor authorizationProcessor;

    public ChallengeData buildNew(DirectoryData directoryData){
        Challenge challenge = new Challenge();
        challenge.setStatus(StatusType.PENDING.toString());

        String token = RandomStringUtils.random(50);

        Base64.Encoder urlEncoder = java.util.Base64.getUrlEncoder().withoutPadding();
        String encoded = urlEncoder.encodeToString(token.getBytes());
        challenge.setToken(encoded);

        ChallengeData challengeData = new ChallengeData(challenge, directoryData.getName());

        return challengeData;
    }

    public ChallengeData processing(ChallengeData challengeData) throws InternalServerException {
        StatusType status = StatusType.getValue(challengeData.getObject().getStatus());
        if(status == StatusType.PENDING || status == StatusType.PROCESSING){
            challengeData.getObject().setStatus(StatusType.PROCESSING.toString());
            return challengeData;
        }else{
            throw new InternalServerException("Challenge invalid status for processing");
        }
    }

    public ChallengeData validation(ChallengeData challengeData, boolean success) throws InternalServerException {
        StatusType status = StatusType.getValue(challengeData.getObject().getStatus());
        if(status == StatusType.PROCESSING){
            if(success){
                challengeData.getObject().setStatus(StatusType.VALID.toString());
                //If challenge is valid, parent authorization should be valid
                authorizationProcessor.challengeMarkedValid(challengeData.getAuthorizationId());
            }else{
                challengeData.getObject().setStatus(StatusType.INVALID.toString());
            }
            challengeData = challengePersistence.save(challengeData);
            return challengeData;
        }else{
            throw new InternalServerException("Challenge invalid status for processing");
        }
    }

}
