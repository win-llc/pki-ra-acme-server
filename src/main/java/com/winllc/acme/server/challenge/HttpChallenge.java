package com.winllc.acme.server.challenge;

import com.winllc.acme.common.contants.StatusType;
import com.winllc.acme.server.exceptions.InternalServerException;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.AuthorizationData;
import com.winllc.acme.common.model.data.ChallengeData;
import com.winllc.acme.server.persistence.AccountPersistence;
import com.winllc.acme.server.persistence.AuthorizationPersistence;
import com.winllc.acme.server.persistence.ChallengePersistence;
import com.winllc.acme.server.process.ChallengeProcessor;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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
    @Autowired
    @Qualifier("appTaskExecutor")
    private TaskExecutor taskExecutor;

    public void verify(ChallengeData challenge) {
        if (challenge.getObject().getStatus().equals(StatusType.PENDING.toString())) {
            try {
                challengeProcessor.processing(challenge);

                challenge.getObject().setStatus(StatusType.PROCESSING.toString());
                challenge = challengePersistence.save(challenge);

                //new VerificationRunner(challenge).run();
                taskExecutor.execute(new VerificationRunner(challenge));
            } catch (Exception e) {
                log.error("Could not verify", e);
            }
        } else {
            log.info("Challenge not in pending state: " + challenge.getId());
        }
    }

    private class VerificationRunner implements Runnable {

        private ChallengeData challenge;

        public VerificationRunner(ChallengeData challenge) {
            this.challenge = challenge;
        }

        @Override
        public void run() {
            boolean success = false;
            int retries = 3;
            int attempts = 0;

            while (attempts < retries && !success) {
                try {

                    Optional<AuthorizationData> authorizationDataOptional = authorizationPersistence.findById(challenge.getAuthorizationId());
                    AuthorizationData authorizationData = authorizationDataOptional.get();
                    Optional<AccountData> accountDataOptional = accountPersistence.findById(authorizationData.getAccountId());

                    /*
                    try {
                        //Sleep 10 seconds before retrying
                        if (!success) Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        log.error("Could not sleep thread", e);
                    }

                     */

                    String urlString = "http://" + authorizationDataOptional.get().getObject().getIdentifier().getValue()
                            + "/.well-known/acme-challenge/" + challenge.getObject().getToken();

                    /*
                    String body = attemptChallenge(urlString);

                    JWK jwk = accountDataOptional.get().buildJwk();
                    log.info(jwk.computeThumbprint().toString());

                    boolean bodyValid = false;
                    String expectedBody = challenge.getObject().getToken() + "." + jwk.computeThumbprint().toString();
                    if (body != null && body.contentEquals(expectedBody)) {
                        bodyValid = true;
                    }

                    //todo add back
                    if (bodyValid) success = true;
                    //success = true;

                     */


                    try {
                        //Sleep 5 seconds before retrying
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        log.error("Could not sleep thread", e);
                    }



                    //success = attemptChallenge(urlString);
                    success = attemptChallenge(urlString);
                    //success = true;

                    challenge = challengeProcessor.validation(challenge, success, false);

                    //challengePersistence.save(challenge);
                } catch (Exception e) {
                    log.error("Could not verify HTTP", e);
                } finally {
                    attempts++;

                    try {
                        //Sleep 5 seconds before retrying
                        if (!success) Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        log.error("Could not sleep thread", e);
                    }



                    if(attempts > retries){
                        try {
                            challengeProcessor.validation(challenge, false, true);
                        } catch (InternalServerException e) {
                            log.error("Could not update challenge", e);
                        }
                    }
                }
            }
        }

        private boolean attemptChallenge(String url) {
            log.info("Attempting challenge at: "+url);
            String result = null;
            HttpGet request = new HttpGet(url);

            //todo remove
            SSLContext sslContext = trustEveryone();

            try (CloseableHttpClient httpClient = HttpClientBuilder.create().setSSLHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            }).setSSLContext(sslContext).build();
                 CloseableHttpResponse response = httpClient.execute(request)) {

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    // return it as a String
                    int responseCode = response.getStatusLine().getStatusCode();
                    if (responseCode == 200) {
                        log.info("Found a valid return code");
                        result = EntityUtils.toString(entity);
                        return true;
                    } else {
                        log.error("Invalid return code: " + responseCode);
                    }
                }
            } catch (Exception e) {
                log.error("Unable to connect", e);
            }
            return false;
        }
    }

    //todo move this somewhere else
    private SSLContext trustEveryone() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }});
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager(){
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) {}
                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {}
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }}}, new SecureRandom());
            return context;
        } catch (Exception e) { // should never happen
            e.printStackTrace();
        }
        return null;
    }
}
