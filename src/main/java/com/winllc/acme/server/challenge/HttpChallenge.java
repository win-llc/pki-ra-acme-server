package com.winllc.acme.server.challenge;

import com.nimbusds.jose.jwk.JWK;
import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.AuthorizationData;
import com.winllc.acme.server.model.data.ChallengeData;
import com.winllc.acme.server.persistence.AccountPersistence;
import com.winllc.acme.server.persistence.AuthorizationPersistence;
import com.winllc.acme.server.persistence.ChallengePersistence;
import com.winllc.acme.server.process.ChallengeProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;

//Section 8.3
@Component
public class HttpChallenge implements ChallengeVerification {

    private static final Logger log = LogManager.getLogger(HttpChallenge.class);

    @Autowired
    private ChallengePersistence challengePersistence;
    @Autowired
    private ChallengeProcessor challengeProcessor;
    @Autowired
    private AuthorizationPersistence authorizationPersistence;
    @Autowired
    private AccountPersistence accountPersistence;

    public void verify(ChallengeData challenge){
        //TODO run verification in separate thread

        try {
            challengeProcessor.processing(challenge);

            challenge.getObject().setStatus(StatusType.PROCESSING.toString());
            challengePersistence.save(challenge);

            new VerificationRunner(challenge).run();
        }catch (Exception e){
            log.error("Could not verify", e);
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
            boolean success = false;

            try {

                Optional<AuthorizationData> authorizationDataOptional = authorizationPersistence.findById(challenge.getAuthorizationId());
                AuthorizationData authorizationData = authorizationDataOptional.get();
                Optional<AccountData> accountDataOptional = accountPersistence.findById(authorizationData.getAccountId());

                String urlString = "http://"+authorizationDataOptional.get().getObject().getIdentifier().getValue()
                        +"/.well-known/acme-challenge/"+challenge.getObject().getToken();
                URL url = new URL(urlString);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setUseCaches(false);
                con.setRequestMethod("GET");

                String body = readResponseBody(con);

                JWK jwk = accountDataOptional.get().buildJwk();
                log.info(jwk.computeThumbprint().toString());

                boolean bodyValid = false;
                String expectedBody = challenge.getObject().getToken()+"."+jwk.computeThumbprint().toString();
                if(body != null && body.contentEquals(expectedBody)){
                    bodyValid = true;
                }

                int responseCode = con.getResponseCode();
                if (responseCode == 200 && bodyValid) success = true;

                challenge = challengeProcessor.validation(challenge, success);

                challengePersistence.save(challenge);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        private String readResponseBody(HttpURLConnection con){
            try {
                StringBuilder result = new StringBuilder();
                BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
                rd.close();
                return result.toString();
            }catch (Exception e){
                log.error("Could not read response", e);
            }
            return null;
        }
    }
}
