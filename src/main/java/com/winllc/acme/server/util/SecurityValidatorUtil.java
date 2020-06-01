package com.winllc.acme.server.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.winllc.acme.common.contants.ProblemType;
import com.winllc.acme.common.contants.StatusType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.exceptions.MalformedRequest;
import com.winllc.acme.common.model.AcmeJWSObject;
import com.winllc.acme.common.model.AcmeURL;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.server.persistence.AccountPersistence;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import net.minidev.json.JSONObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SecurityValidatorUtil {

    private static final Logger log = LogManager.getLogger(SecurityValidatorUtil.class);

    @Autowired
    private DirectoryDataService directoryDataService;
    @Autowired
    private AccountPersistence accountPersistence;

    public <T> PayloadAndAccount<T> verifyJWSAndReturnPayloadForExistingAccount(HttpServletRequest httpServletRequest, Class<T> clazz) throws AcmeServerException {
        AcmeJWSObject jwsObject = getJWSObjectFromHttpRequest(httpServletRequest);

        //Should contain account URL
        String kid;

        if (StringUtils.isNotBlank(jwsObject.getHeader().getKeyID())) {
            AcmeURL acmeURL = new AcmeURL(jwsObject.getHeader().getKeyID());
            kid = acmeURL.getObjectId().get();
        } else {
            Optional<AccountData> firstByJwkEquals = accountPersistence.findFirstByJwkEquals(jwsObject.getHeader().getJWK().toString());
            if (firstByJwkEquals.isPresent()) {
                AccountData accountData = firstByJwkEquals.get();
                kid = accountData.getId();
            } else {
                throw new AcmeServerException(ProblemType.ACCOUNT_DOES_NOT_EXIST);
            }
        }


        return verifyJWSAndReturnPayloadForExistingAccount(jwsObject, httpServletRequest.getRequestURL().toString(), kid, clazz);
    }

    public <T> PayloadAndAccount<T> verifyJWSAndReturnPayloadForExistingAccount(HttpServletRequest httpServletRequest,
                                                                                String accountId, Class<T> clazz) throws AcmeServerException {
        AcmeJWSObject jwsObject = getJWSObjectFromHttpRequest(httpServletRequest);
        return verifyJWSAndReturnPayloadForExistingAccount(jwsObject, httpServletRequest.getRequestURL().toString(), accountId, clazz);
    }

    public <T> PayloadAndAccount<T> verifyJWSAndReturnPayloadForExistingAccount(AcmeJWSObject jwsObject, HttpServletRequest httpServletRequest,
                                                                                Class<T> clazz) throws AcmeServerException {
        AcmeURL kid = new AcmeURL(jwsObject.getHeader().getKeyID());
        return verifyJWSAndReturnPayloadForExistingAccount(jwsObject, httpServletRequest.getRequestURL().toString(), kid.getObjectId().get(), clazz);
    }

    public <T> PayloadAndAccount<T> verifyJWSAndReturnPayloadForExistingAccount(AcmeJWSObject jwsObject, String requestUrl,
                                                                                String accountId, Class<T> clazz) throws AcmeServerException {
        //Section 6.2
        if (!jwsObject.hasValidHeaderFields()) {
            log.info("Invalid header field found");
            throw new AcmeServerException(ProblemType.MALFORMED);
        }

        Optional<DirectoryData> directoryDataOptional = directoryDataService.getByName(jwsObject.getHeaderAcmeUrl().getDirectoryIdentifier());
        if (directoryDataOptional.isPresent()) {
            DirectoryData directoryData = directoryDataOptional.get();
            //The URL must match the URL in the JWS Header
            //Section 6.4
            String headerUrl = jwsObject.getHeaderAcmeUrl().getUrl();

            if (!headerUrl.contentEquals(requestUrl)) {
                log.debug("Header URL and Request URL did not match");
                throw new AcmeServerException(ProblemType.UNAUTHORIZED, "Header and Request URLs did not match");
            }

            Optional<AccountData> optionalAccount = accountPersistence.findById(accountId);
            if (optionalAccount.isPresent()) {
                AccountData accountData = optionalAccount.get();

                //Section 7.3.6
                if (accountData.getObject().getStatus().contentEquals(StatusType.DEACTIVATED.toString())) {
                    throw new AcmeServerException(ProblemType.UNAUTHORIZED);
                }

                JWK accountJWK;
                try {
                    accountJWK = JWK.parse(accountData.getJwk());
                } catch (ParseException e) {
                    throw new AcmeServerException(ProblemType.SERVER_INTERNAL, "Unable to parse account JWK");
                }

                boolean verified = verifyJWS(jwsObject, accountJWK);

                //Verify signature
                if (!verified) {
                    throw new AcmeServerException(ProblemType.MALFORMED);
                }

                String nonce = jwsObject.getNonce();
                //Verify nonce has not been used
                if (!NonceUtil.checkNonceUsed(nonce)) {
                    NonceUtil.markNonceUsed(nonce);
                } else {
                    //NONCE has been used before, possible replay attack
                    //Section 6.5.2
                    throw new MalformedRequest();
                }

                T obj = getPayloadFromJWSObject(jwsObject, clazz);

                return new PayloadAndAccount<>(obj, accountData, directoryData);
            } else {
                throw new AcmeServerException(ProblemType.ACCOUNT_DOES_NOT_EXIST);
            }
        } else {
            throw new AcmeServerException(ProblemType.SERVER_INTERNAL, "Could not find DirectoryData");
        }
    }

    public static <T> T getPayloadFromJWSObject(JWSObject jwsObject, Class<T> clazz) throws AcmeServerException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            if (StringUtils.isNotBlank(jwsObject.getPayload().toString())) {
                JSONObject jsonObject = jwsObject.getPayload().toJSONObject();
                return objectMapper.readValue(jsonObject.toJSONString(), clazz);
            } else {
                return (T) "";
            }
        } catch (Exception e) {
            log.error("Could not get payload from JWS", e);
            throw new AcmeServerException(ProblemType.SERVER_INTERNAL, "Unexpected JOSE payload type");
        }
    }

    public static AcmeJWSObject getJWSObjectFromHttpRequest(HttpServletRequest httpServletRequest) throws AcmeServerException {
        String body;
        try {
            body = httpServletRequest.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            log.info("Request Body for: " + httpServletRequest.getRequestURI());
            log.info(body);
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
        JWK jwk = jwsObject.getHeader().getJWK();
        return verifyJWS(jwsObject, jwk);
    }

    public static boolean verifyJWS(JWSObject jwsObject, JWK jwkToVerify) throws AcmeServerException {
        try {
            JWSVerifier verifier = buildVerifierFromJWS(jwsObject, jwkToVerify);
            return jwsObject.verify(verifier);
        } catch (JOSEException e) {
            log.error("Could not build JWS verifier", e);
        }
        return false;
    }


    //TODO better understand this
    private static JWSVerifier buildVerifierFromJWS(JWSObject jwsObject, JWK jwkToVerify) throws JOSEException, AcmeServerException {
        JWSAlgorithm jwsAlgorithm = jwsObject.getHeader().getAlgorithm();

        if (JWSAlgorithm.Family.EC.contains(jwsAlgorithm)) {
            return new ECDSAVerifier((ECKey) jwsObject.getHeader().getJWK().toPublicJWK());
        } else if (JWSAlgorithm.Family.ED.contains(jwsAlgorithm)) {
            throw new RuntimeException("Not supported: " + jwsAlgorithm.getName());
        } else if (JWSAlgorithm.Family.HMAC_SHA.contains(jwsAlgorithm)) {
            throw new RuntimeException("Not supported: " + jwsAlgorithm.getName());
        } else if (JWSAlgorithm.Family.RSA.contains(jwsAlgorithm)) {
            return new RSASSAVerifier((RSAKey) jwkToVerify.toPublicJWK());
        } else if (JWSAlgorithm.Family.SIGNATURE.contains(jwsAlgorithm)) {
            throw new RuntimeException("Not supported: " + jwsAlgorithm.getName());
        }
        throw new AcmeServerException(ProblemType.BAD_SIGNATURE_ALGORITHM);
    }

}
