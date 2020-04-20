package com.winllc.acme.server;

import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.external.ExternalAccountProvider;
import com.winllc.acme.server.model.AcmeJWSObject;

import java.util.List;

public class MockExternalAccountProvider implements ExternalAccountProvider {
    @Override
    public String getName() {
        return "mock";
    }

    @Override
    public boolean verifyExternalAccountJWS(AcmeJWSObject jwsObject) throws AcmeServerException {
        return true;
    }

    @Override
    public List<String> getCanIssueToDomainsForExternalAccount(String accountKeyIdentifier) {
        return null;
    }

    @Override
    public String getAccountVerificationUrl() {
        return null;
    }

    @Override
    public String getAccountValidationRulesUrl() {
        return null;
    }

    @Override
    public List<String> getPreAuthorizationIdentifiers(String accountKeyIdentifier) {
        return null;
    }
}