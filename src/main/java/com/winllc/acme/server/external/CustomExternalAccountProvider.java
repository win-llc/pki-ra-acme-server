package com.winllc.acme.server.external;

import com.nimbusds.jose.JWSObject;
import com.winllc.acme.server.model.Directory;

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
    public boolean verifyExternalAccountJWS(JWSObject jwsObject) {
        return false;
    }
}
