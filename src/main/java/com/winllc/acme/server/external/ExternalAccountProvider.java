package com.winllc.acme.server.external;

import com.nimbusds.jose.JWSObject;
import com.winllc.acme.server.model.Directory;

//Section 7.3.4
public interface ExternalAccountProvider {

    Directory getLinkedDirectory();
    ExternalAccount lookupByKeyIdentifier(String kid);
    boolean verifyExternalAccountJWS(JWSObject jwsObject);
}
