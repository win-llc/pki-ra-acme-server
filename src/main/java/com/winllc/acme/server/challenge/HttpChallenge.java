package com.winllc.acme.server.challenge;

import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.exceptions.InternalServerException;
import com.winllc.acme.server.model.data.ChallengeData;
import com.winllc.acme.server.persistence.ChallengePersistence;
import com.winllc.acme.server.process.ChallengeProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

//Section 8.3
@Component
public class HttpChallenge implements ChallengeVerification {

    @Autowired
    private ChallengePersistence challengePersistence;
    @Autowired
    private ChallengeProcessor challengeProcessor;

    public void verify(ChallengeData challenge){
        //TODO run verification in separate thread

        try {
            challengeProcessor.processing(challenge);

            challenge.getObject().setStatus(StatusType.PROCESSING.toString());
            challengePersistence.save(challenge);

            new VerificationRunner(challenge).run();
        }catch (InternalServerException e){
            e.printStackTrace();
        }
    }

    private class VerificationRunner implements Runnable {

        private ChallengeData challenge;

        public VerificationRunner(ChallengeData challenge) {
            this.challenge = challenge;
        }

        @Override
        public void run() {
            //TODO
            boolean success = true;

            try {
                challenge = challengeProcessor.validation(challenge, success);

                challengePersistence.save(challenge);
            }catch (InternalServerException e){
                e.printStackTrace();
            }
        }
    }
}
