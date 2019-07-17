package com.winllc.acme.server.external;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.model.AcmeJWSObject;
import com.winllc.acme.server.util.AppUtil;

public abstract class ExternalAccountProviderImpl implements ExternalAccountProvider {

    /*
    The ACME client then computes a binding JWS to indicate the external account holder’s approval of the ACME account key.
    The payload of this JWS is the ACME account key being registered, in JWK form. The protected header of the JWS MUST meet the following criteria:

The “alg” field MUST indicate a MAC-based algorithm
The “kid” field MUST contain the key identifier provided by the CA
The “nonce” field MUST NOT be present
The “url” field MUST be set to the same value as the outer JWS
     */
    @Override
    public boolean verifyExternalAccountJWS(AcmeJWSObject outerObject) throws AcmeServerException {
        AcmeJWSObject innerObject = AppUtil.getPayloadFromJWSObject(outerObject, AcmeJWSObject.class);

        boolean valid = true;
        JWSHeader header = outerObject.getHeader();

        //The “alg” field MUST indicate a MAC-based algorithm
        if(!JWSAlgorithm.Family.HMAC_SHA.contains(header.getAlgorithm())){
            valid = false;
        }

        //The “kid” field MUST contain the key identifier provided by the CA
        //TODO

        //The “nonce” field MUST NOT be present
        if(outerObject.getNonce() != null){
            valid = false;
        }

        //The “url” field MUST be set to the same value as the outer JWS
        String innerUrl = innerObject.getHeaderAcmeUrl().toString();
        String outerUrl = outerObject.getHeaderAcmeUrl().toString();
        if(!innerUrl.equalsIgnoreCase(outerUrl)){
            valid = false;
        }

        return valid;
    }

    /*
    To verify the account binding, the CA MUST take the following steps:

    Verify that the value of the field is a well-formed JWS
    Verify that the JWS protected field meets the above criteria
    Retrieve the MAC key corresponding to the key identifier in the “kid” field
    Verify that the MAC on the JWS verifies using that MAC key
    Verify that the payload of the JWS represents the same key as was used to verify the outer JWS (i.e., the “jwk” field of the outer JWS)
     */
    private boolean verifyAccountBinding(AcmeJWSObject jwsObject){
        //TODO
        //Send JWS to verificationURL
        getAccountVerificationUrl();

        return false;
    }
}
