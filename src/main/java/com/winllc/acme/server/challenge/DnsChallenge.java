package com.winllc.acme.server.challenge;

import com.winllc.acme.server.model.data.ChallengeData;
import org.springframework.stereotype.Component;

//Section 8.4
@Component
public class DnsChallenge implements ChallengeVerification {

    public void verify(ChallengeData challenge){
        //TODO

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

        }

    }
}
