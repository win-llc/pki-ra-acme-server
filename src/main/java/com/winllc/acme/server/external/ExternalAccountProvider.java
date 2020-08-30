package com.winllc.acme.server.external;

import com.nimbusds.jose.JWSObject;
import com.winllc.acme.common.CertIssuanceValidationResponse;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.exceptions.InternalServerException;

import java.util.List;

//Section 7.3.4
public interface ExternalAccountProvider {

    String getName();
    boolean verifyAccountBinding(JWSObject jwsObject, JWSObject outerJWSObject) throws AcmeServerException;

    List<String> getCanIssueToDomainsForExternalAccount(String accountKeyIdentifier) throws Exception;
    //For verification of JWS sent from client
    String getAccountVerificationUrl();
    String getAccountValidationRulesUrl();

    List<String> getPreAuthorizationIdentifiers(String accountKeyIdentifier) throws InternalServerException;
    CertIssuanceValidationResponse getValidationRules(AccountData accountData) throws AcmeServerException;
}
