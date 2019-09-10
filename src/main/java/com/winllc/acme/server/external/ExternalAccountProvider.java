package com.winllc.acme.server.external;

import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.model.AcmeJWSObject;

//Section 7.3.4
public interface ExternalAccountProvider {

    String getName();
    boolean verifyExternalAccountJWS(AcmeJWSObject jwsObject) throws AcmeServerException;

    //For verification of JWS sent from client
    String getAccountVerificationUrl();
}
