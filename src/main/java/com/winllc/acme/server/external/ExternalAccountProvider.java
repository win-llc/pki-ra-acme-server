package com.winllc.acme.server.external;

import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.model.AcmeJWSObject;

import java.util.List;

//Section 7.3.4
public interface ExternalAccountProvider {

    String getName();
    boolean verifyExternalAccountJWS(AcmeJWSObject jwsObject) throws AcmeServerException;

    List<String> getCanIssueToDomainsForExternalAccount(String accountKeyIdentifier);
    //For verification of JWS sent from client
    String getAccountVerificationUrl();
    String getAccountValidationRulesUrl();

    List<String> getPreAuthorizationIdentifiers(String accountKeyIdentifier);
}
