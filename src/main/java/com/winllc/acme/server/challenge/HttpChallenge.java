package com.winllc.acme.server.challenge;

import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.model.Challenge;
import com.winllc.acme.server.model.data.ChallengeData;
import com.winllc.acme.server.persistence.ChallengePersistence;

//Section 8.3
public class HttpChallenge implements ChallengeVerification {

    private ChallengePersistence challengePersistence;

    public void verify(ChallengeData challenge){
        //TODO run verification in separate thread

        challenge.getObject().setStatus(StatusType.PROCESSING.toString());
        challengePersistence.save(challenge);

        new VerificationRunner(challenge).run();
    }

    private class VerificationRunner implements Runnable {

        private ChallengeData challenge;

        public VerificationRunner(ChallengeData challenge) {
            this.challenge = challenge;
        }

        @Override
        public void run() {
            //TODO
            boolean success = false;

            if(success){
                challenge.getObject().setStatus(StatusType.VALID.toString());
            }else{
                challenge.getObject().setStatus(StatusType.INVALID.toString());
            }
            challengePersistence.save(challenge);
        }
    }
}
