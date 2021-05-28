package com.winllc.acme.server.challenge;

import com.winllc.acme.common.contants.StatusType;
import com.winllc.acme.common.model.acme.Identifier;
import com.winllc.acme.common.model.data.ChallengeData;
import com.winllc.acme.server.persistence.ChallengePersistence;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;


//Section 8.4
@Component
public class DnsChallenge implements ChallengeVerification {

    private static final Logger log = LogManager.getLogger(DnsChallenge.class);

    private static final String prependedURL = "_acme-challenge.";

    @Autowired
    private ChallengePersistence challengePersistence;
    //@Autowired
    //private ChallengeProcessor challengeProcessor;


    public void verify(ChallengeData challenge) {
        //TODO
        try {
            //challengeProcessor.processing(challenge);

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

        }

        private String findTXTRecord(Identifier identifier) throws Exception {
            try {
                final Lookup lookup = new Lookup(prependedURL + identifier.getValue(), Type.TXT);
                lookup.setResolver(new SimpleResolver());
                lookup.setCache(null);
                final Record[] records = lookup.run();
                if (lookup.getResult() == Lookup.SUCCESSFUL) {
                    final StringBuilder builder = new StringBuilder();
                    for (Record record : records) {
                        final TXTRecord txtRecord = (TXTRecord) record;
                        builder.delete(0, builder.length());
                        for (String o : (Iterable<String>) txtRecord.getStrings()) {
                            builder.append(o);
                        }
                        final String txt = builder.toString();
                        // TODO
                        return txt;
                    }
                }
            } catch (Exception e) {
                log.error("Could not lookup TXT record for: " + identifier.getValue(), e);
                throw e;
            }
            throw new Exception("No response");
        }

    }
}
