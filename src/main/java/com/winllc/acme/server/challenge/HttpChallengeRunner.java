package com.winllc.acme.server.challenge;

import com.nimbusds.jose.jwk.JWK;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.AuthorizationData;
import com.winllc.acme.common.model.data.ChallengeData;
import com.winllc.acme.server.transaction.AbstractTransaction;
import com.winllc.acme.server.transaction.AuthorizationTransaction;
import com.winllc.acme.server.transaction.CertIssuanceTransaction;
import com.winllc.acme.server.transaction.ChallengeDataWrapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

//Section 8.3
public class HttpChallengeRunner {

    private static final Logger log = LogManager.getLogger(HttpChallengeRunner.class);

    public static class VerificationRunner implements Runnable {

        private ChallengeDataWrapper challengeWrapper;
        private AuthorizationTransaction certIssuanceTransaction;

        public VerificationRunner(String challengeId, AuthorizationTransaction certIssuanceTransaction) {
            this.certIssuanceTransaction = certIssuanceTransaction;
            this.challengeWrapper = this.certIssuanceTransaction.retrieveChallengeData(challengeId);
        }

        @Override
        public void run() {
            boolean success = false;
            int retries = 4;
            int attempts = 0;

            ChallengeData challenge = this.challengeWrapper.getData();
            this.challengeWrapper.markProcessing();

            while (attempts < retries && !success) {
                try {

                    AuthorizationData authorizationData = this.certIssuanceTransaction.retrieveAuthorizationData(challenge.getAuthorizationId());
                    AccountData accountData = this.certIssuanceTransaction.getTransactionContext().getAccountData();

                    String urlString = "http://" + authorizationData.getObject().getIdentifier().getValue()
                            + "/.well-known/acme-challenge/" + challenge.getObject().getToken();

                    String body = attemptChallenge(urlString);

                    JWK jwk = accountData.buildJwk();
                    log.info(jwk.computeThumbprint().toString());

                    boolean bodyValid = false;
                    String expectedBody = challenge.getObject().getToken() + "." + jwk.computeThumbprint().toString();
                    if (body != null && body.contentEquals(expectedBody)) {
                        bodyValid = true;
                    }

                    if (bodyValid){
                        success = true;
                        certIssuanceTransaction.markChallengeComplete(challenge.getId());
                        break;
                    }else{
                        try {
                            //Sleep 5 seconds before retrying
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            log.error("Could not sleep thread", e);
                        }
                    }
                } catch (Exception e) {
                    log.error("Could not verify HTTP", e);
                } finally {
                    attempts++;

                    if(attempts >= retries && !success){
                        try {
                            //challengeProcessor.validation(challenge, false, true);
                        } catch (Exception e) {
                            log.error("Could not update challenge", e);
                        }
                    }
                }
            }
        }

        private static String attemptChallenge(String url) {
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
                        return result;
                    } else {
                        log.error("Invalid return code: " + responseCode);
                    }
                }
            } catch (Exception e) {
                log.error("Unable to connect", e);
            }
            return null;
        }
    }

    //todo move this somewhere else
    private static SSLContext trustEveryone() {
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
