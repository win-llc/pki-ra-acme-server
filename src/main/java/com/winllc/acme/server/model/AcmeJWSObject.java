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

    /**
     * The JWS MUST be in the Flattened JSON Serialization [RFC7515]
     * The JWS MUST NOT have multiple signatures
     * The JWS Unencoded Payload Option [RFC7797] MUST NOT be used
     * The JWS Unprotected Header [RFC7515] MUST NOT be used
     * The JWS Payload MUST NOT be detached
     * The JWS Protected Header MUST include the following fields:
     * “alg” (Algorithm)
     * This field MUST NOT contain “none” or a Message Authentication Code (MAC) algorithm (e.g. one in which the algorithm registry description mentions MAC/HMAC).
     * “nonce” (defined in Section 6.5)
     * “url” (defined in Section 6.4)
     * Either “jwk” (JSON Web Key) or “kid” (Key ID) as specified below
     * @return
     */
    public boolean hasValidHeaderFields(){
        JWSHeader header = getHeader();

        if(header.getAlgorithm() == null) return false;
        if(header.getAlgorithm().getName().contentEquals("none")) return false;
        if(JWSAlgorithm.Family.HMAC_SHA.contains(header.getAlgorithm())) return false;
        if(header.getCustomParam("nonce") == null) return false;
        if(header.getCustomParam("url") == null) return false;
        if(header.getJWK() == null && header.getKeyID() == null){
            return false;
        }else return header.getJWK() == null || header.getKeyID() == null;

    }

}
