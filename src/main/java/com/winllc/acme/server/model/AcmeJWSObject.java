package com.winllc.acme.server.model;

import com.nimbusds.jose.*;
import com.nimbusds.jose.util.Base64URL;

import java.text.ParseException;

public class AcmeJWSObject extends JWSObject {
    public AcmeJWSObject(JWSHeader header, Payload payload) {
        super(header, payload);
    }

    public AcmeJWSObject(Base64URL firstPart, Base64URL secondPart, Base64URL thirdPart) throws ParseException {
        super(firstPart, secondPart, thirdPart);
    }

    public AcmeURL getHeaderAcmeUrl(){
        return new AcmeURL(getHeader().getCustomParam("url").toString());
    }

    public String getNonce(){
        return getHeader().getCustomParam("nonce").toString();
    }

    public static AcmeJWSObject parse(final String s)
            throws ParseException {

        Base64URL[] parts = JOSEObject.split(s);

        if (parts.length != 3) {

            throw new ParseException("Unexpected number of Base64URL parts, must be three", 0);
        }

        return new AcmeJWSObject(parts[0], parts[1], parts[2]);
    }

    //Section 6.2
    public boolean hasValidHeaderFields(){
        if(getHeader().getCustomParam("alg") != null &&
                getHeader().getCustomParam("nonce") != null &&
                getHeader().getCustomParam("url") != null &&
                (getHeader().getCustomParam("jwk") != null || getHeader().getCustomParam("kid") != null)){
            boolean valid = true;
            String alg = getHeader().getCustomParam("alg").toString();
            if(alg.equalsIgnoreCase("none") && JWSAlgorithm.Family.HMAC_SHA.contains(JWSAlgorithm.parse(alg))){
                valid = false;
            }

            if(getHeader().getCustomParam("jwk") != null && getHeader().getCustomParam("kid") != null){
                valid = false;
            }
            return valid;
        }
        return false;
    }
}
