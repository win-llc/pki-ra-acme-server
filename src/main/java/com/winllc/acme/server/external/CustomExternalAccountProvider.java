package com.winllc.acme.server.external;

import com.nimbusds.jose.JWSObject;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.model.AcmeJWSObject;
import com.winllc.acme.server.model.acme.Directory;

public class CustomExternalAccountProvider implements ExternalAccountProvider {

    @Override
    public Directory getLinkedDirectory() {
        return null;
    }

    @Override
    public ExternalAccount lookupByKeyIdentifier(String kid) {
        return null;
    }

    @Override
    public boolean verifyExternalAccountJWS(AcmeJWSObject jwsObject) throws AcmeServerException {
        return false;
    }

    @Override
    public String getAccountVerificationUrl() {
        return null;
    }
}
