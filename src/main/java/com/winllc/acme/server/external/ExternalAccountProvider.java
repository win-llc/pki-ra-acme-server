package com.winllc.acme.server.external;

import com.nimbusds.jose.JWSObject;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.model.AcmeJWSObject;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.data.DirectoryData;

//Section 7.3.4
public interface ExternalAccountProvider {

    String getName();
    DirectoryData getLinkedDirectory();
    boolean verifyExternalAccountJWS(AcmeJWSObject jwsObject) throws AcmeServerException;

    //For verification of JWS sent from client
    String getAccountVerificationUrl();
}
