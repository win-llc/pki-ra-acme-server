package com.winllc.acme.server.process;

import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.exceptions.InternalServerException;
import com.winllc.acme.server.model.acme.Challenge;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.data.AuthorizationData;
import com.winllc.acme.server.model.data.ChallengeData;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.persistence.ChallengePersistence;

import java.util.List;

public class ChallengeProcessor implements AcmeDataProcessor<ChallengeData> {

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

    private ChallengePersistence challengePersistence;
    private AuthorizationProcessor authorizationProcessor;

    public ChallengeData buildNew(DirectoryData directoryData){
        Challenge challenge = new Challenge();
        challenge.setStatus(StatusType.PENDING.toString());

        ChallengeData challengeData = new ChallengeData(challenge, directoryData);

        return challengeData;
    }

    public ChallengeData processing(ChallengeData challengeData) throws InternalServerException {
        StatusType status = StatusType.valueOf(challengeData.getObject().getStatus());
        if(status == StatusType.PENDING || status == StatusType.PROCESSING){
            challengeData.getObject().setStatus(StatusType.PROCESSING.toString());
            return challengeData;
        }else{
            throw new InternalServerException("Challenge invalid status for processing");
        }
    }

    public ChallengeData validation(ChallengeData challengeData, boolean success) throws InternalServerException {
        StatusType status = StatusType.valueOf(challengeData.getObject().getStatus());
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

    public List<ChallengeData> getCurrentChallengesForAuthorization(AuthorizationData authorizationData){
        //TODO
        List<ChallengeData> challengeDataList = challengePersistence.getAllChallengesForAuthorization(authorizationData);
        return null;
    }
}
