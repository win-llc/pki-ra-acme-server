package com.winllc.acme.server.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.exceptions.MalformedRequest;
import com.winllc.acme.server.model.AcmeJWSObject;
import com.winllc.acme.server.model.AcmeURL;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.persistence.AccountPersistence;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.ParseException;
import java.util.Optional;
import java.util.stream.Collectors;

public class AppUtil {

    private static final Logger log = LogManager.getLogger(AppUtil.class);

    public static String generateRandomString(int length) {
        boolean useLetters = true;
        boolean useNumbers = true;

        return RandomStringUtils.random(length, useLetters, useNumbers);
    }

    public static String generateNonce(){
        String nonce = AppUtil.generateRandomString(10);
        Application.unUsedNonces.add(nonce);
        return nonce;
    }

    public static <T> PayloadAndAccount<T> verifyJWSAndReturnPayloadForExistingAccount(HttpServletRequest httpServletRequest, Class<T> clazz) throws AcmeServerException {
        AcmeJWSObject jwsObject = getJWSObjectFromHttpRequest(httpServletRequest);

        //Should contain account URL
        AcmeURL kid = new AcmeURL(jwsObject.getHeader().getKeyID());

        return verifyJWSAndReturnPayloadForExistingAccount(httpServletRequest, kid.getObjectId().get(), clazz);
    }

    public static <T> PayloadAndAccount<T> verifyJWSAndReturnPayloadForExistingAccount(HttpServletRequest httpServletRequest,
                                                                                       String accountId, Class<T> clazz) throws AcmeServerException {
        AcmeJWSObject jwsObject = getJWSObjectFromHttpRequest(httpServletRequest);
        DirectoryData directoryData = Application.directoryDataMap.get(jwsObject.getHeaderAcmeUrl().getDirectoryIdentifier());
        //The URL must match the URL in the JWS Header
        //Section 6.4
        String headerUrl = jwsObject.getHeaderAcmeUrl().getUrl();
        if(!headerUrl.contentEquals(httpServletRequest.getRequestURL())){
            throw new AcmeServerException(ProblemType.UNAUTHORIZED);
        }

        Optional<AccountData> optionalAccount = new AccountPersistence().getByAccountId(accountId);
        if(optionalAccount.isPresent()) {
            AccountData accountData = optionalAccount.get();

            //Section 7.3.6
            if(accountData.getObject().getStatus().contentEquals(StatusType.DEACTIVATED.toString())){
                throw new AcmeServerException(ProblemType.UNAUTHORIZED);
            }

            JWK accountJWK;
            try {
                accountJWK = JWK.parse(accountData.getJwk());
            } catch (ParseException e) {
                throw new AcmeServerException(ProblemType.SERVER_INTERNAL, "Unable to parse account JWK");

            }

            boolean verified = verifyJWS(jwsObject);

            //Verify signature
            if (!verified) {
                throw new AcmeServerException(ProblemType.MALFORMED);
            }

            //Check if account key and request JWK match
            if (jwsObject.getHeader().getJWK().equals(accountJWK)) {
                throw new AcmeServerException(ProblemType.UNAUTHORIZED);
            }

            String nonce = jwsObject.getNonce();
            //Verify nonce has not been used
            if (Application.unUsedNonces.contains(nonce) && !Application.usedNonces.contains(nonce)) {
                Application.usedNonces.add(nonce);
            } else {
                //NONCE has been used before, possible replay attack
                //Section 6.5.2
                throw new MalformedRequest();
            }

            T obj = getPayloadFromJWSObject(jwsObject, clazz);

            return new PayloadAndAccount<>(obj, accountData, directoryData);
        }else{
            throw new AcmeServerException(ProblemType.ACCOUNT_DOES_NOT_EXIST);
        }
    }

    public static <T> T getPayloadFromJWSObject(JWSObject jwsObject, Class<T> clazz) throws AcmeServerException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(jwsObject.getPayload().toJSONObject().toJSONString(), clazz);
        } catch (IOException e) {
            throw new AcmeServerException(ProblemType.SERVER_INTERNAL, "Unexpected JOSE payload type");
        }
    }

    public static AcmeJWSObject getJWSObjectFromHttpRequest(HttpServletRequest httpServletRequest) throws AcmeServerException {
        String body;
        try {
            body = httpServletRequest.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new AcmeServerException(ProblemType.SERVER_INTERNAL, "Unable to read HTTP request data");
        }

        AcmeJWSObject jwsObject;
        try {
            jwsObject = AcmeJWSObject.parse(body);
        } catch (ParseException e) {
            throw new AcmeServerException(ProblemType.SERVER_INTERNAL, "Unable to parse request JWK");
        }
        return jwsObject;
    }

    //Ensure JWS signed by public key in JWK
    public static boolean verifyJWS(JWSObject jwsObject) throws AcmeServerException {
        try {
            JWSVerifier verifier = buildVerifierFromJWS(jwsObject);
            return jwsObject.verify(verifier);
        } catch (JOSEException e) {
            e.printStackTrace();
        }
        return false;
    }


    //TODO better understand this
    public static JWSVerifier buildVerifierFromJWS(JWSObject jwsObject) throws JOSEException, AcmeServerException {
        JWSAlgorithm jwsAlgorithm = jwsObject.getHeader().getAlgorithm();

        if(JWSAlgorithm.Family.EC.contains(jwsAlgorithm)){
            return new ECDSAVerifier((ECKey) jwsObject.getHeader().getJWK().toPublicJWK());
        }else if(JWSAlgorithm.Family.ED.contains(jwsAlgorithm)){

        }else if(JWSAlgorithm.Family.HMAC_SHA.contains(jwsAlgorithm)){

        }else if(JWSAlgorithm.Family.RSA.contains(jwsAlgorithm)){
            return new RSASSAVerifier((RSAKey) jwsObject.getHeader().getJWK().toPublicJWK());
        }else if(JWSAlgorithm.Family.SIGNATURE.contains(jwsAlgorithm)){

        }
        throw new AcmeServerException(ProblemType.BAD_SIGNATURE_ALGORITHM);
    }

}
