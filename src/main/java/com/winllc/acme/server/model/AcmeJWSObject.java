package com.winllc.acme.server.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.util.Base64URL;
import org.jose4j.base64url.Base64Url;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

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

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, String> objMap = objectMapper.readValue(s, Map.class);
            Base64URL part1 = new Base64URL(objMap.get("protected"));
            Base64URL part2 = new Base64URL(objMap.get("payload"));
            Base64URL part3 = new Base64URL(objMap.get("signature"));

            return new AcmeJWSObject(part1, part2, part3);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ParseException("fail", 0);
        }
    }

    //Section 6.2
    public boolean hasValidHeaderFields(){
        if(getHeader().getAlgorithm() != null &&
                getHeader().getCustomParam("nonce") != null &&
                getHeader().getCustomParam("url") != null &&
                (getHeader().getJWK() != null || getHeader().getCustomParam("kid") != null)){
            boolean valid = true;
            JWSAlgorithm alg = getHeader().getAlgorithm();
            if(JWSAlgorithm.Family.HMAC_SHA.contains(alg)){
                valid = false;
            }

            if(getHeader().getJWK() != null && getHeader().getCustomParam("kid") != null){
                valid = false;
            }
            return valid;
        }
        return false;
    }
}
