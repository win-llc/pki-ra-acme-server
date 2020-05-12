package com.winllc.acme.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import com.nimbusds.jose.jwk.gen.OctetSequenceKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.winllc.acme.server.contants.ChallengeType;
import com.winllc.acme.server.contants.IdentifierType;
import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.model.AcmeJWSObject;
import com.winllc.acme.server.model.acme.*;
import com.winllc.acme.server.model.data.*;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.bouncycastle.jcajce.provider.digest.Skein;
import org.jose4j.keys.HmacKey;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.text.ParseException;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MockUtils {

    public static final String mockIdentifier = "test.winllc.com";
    public static final Identifier identifier = new Identifier(IdentifierType.DNS, mockIdentifier);

    private static RSAKey rsaJWK;
    private static JWK hmacJwk;
    private static SecretKey hmacKey;
    private static AccountData accountData;

    static {
        try {
            rsaJWK = new RSAKeyGenerator(2048)
                    .keyID("123")
                    .generate();

            hmacKey = KeyGenerator.getInstance("HmacSha256").generateKey();
            hmacJwk = new OctetSequenceKey.Builder(hmacKey)
                    .keyID(UUID.randomUUID().toString()) // give the key some ID (optional)
                    .algorithm(JWSAlgorithm.HS256) // indicate the intended key alg (optional)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Account account = new Account();
        account.setStatus(StatusType.VALID.toString());
        account.setOrders("http://localhost/acme-test/orders/1");
        accountData = new AccountData(account, "acme-test");
        accountData.setJwk(rsaJWK.toPublicJWK().toJSONString());
    }

    public static AcmeJWSObject buildCustomAcmeJwsObject(Object jsonObject, String url)
            throws JsonProcessingException, JOSEException, ParseException {
        JWSObject jwsObject = buildCustomJwsObject(jsonObject, url);
        String jsonString = MockUtils.jwsObjectAsString(jwsObject);
        return AcmeJWSObject.parse(jsonString);
    }

    public static JWSObject buildCustomJwsObject(Object jsonObject, String url) throws JOSEException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return buildCustomJwsObject(objectMapper.writeValueAsString(jsonObject), url);
    }

    public static JWSObject buildCustomJwsObject(String jsonPayload, String url) throws JOSEException {
        return buildCustomJwsObject(jsonPayload, url, null);
    }

    public static JWSObject buildCustomJwsObject(String jsonPayload, String url, String kid) throws JOSEException {
        return buildCustomJwsObject(jsonPayload, url, kid, JWSAlgorithm.RS256, true);
    }

    public static JWSObject buildCustomJwsObject(String jsonPayload, String url, String kid, JWSAlgorithm jwsAlgorithm, boolean hasNonce) throws JOSEException {

        JWSHeader.Builder builder;
        JWSSigner signer = new RSASSASigner(rsaJWK);

        if(JWSAlgorithm.Family.HMAC_SHA.contains(jwsAlgorithm)){
            builder = new JWSHeader.Builder(jwsAlgorithm)
                    .jwk(hmacJwk.toPublicJWK());
            signer = new MACSigner(hmacKey);
        }else{
            builder = new JWSHeader.Builder(jwsAlgorithm)
                    .jwk(rsaJWK.toPublicJWK());
            signer = new RSASSASigner(rsaJWK);
        }

        if(url != null){
            builder.customParam("url", url);
        }

        if(hasNonce) {
            builder.customParam("nonce", "1");
        }

        if(kid != null){
            builder.keyID(kid);
        }

        JWSHeader jwsHeader = builder.build();

        Payload payload = new Payload(jsonPayload);
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);

        jwsObject.sign(signer);

        return jwsObject;
    }

    public static String jwsObjectAsString(JWSObject jwsObject){
        return "{\"protected\":\"" + jwsObject.getHeader().toBase64URL() + "\"," +
                " \"payload\":\"" + jwsObject.getPayload().toBase64URL() + "\", \"signature\":\"" + jwsObject.getSignature() + "\"}";
    }

    public static DirectoryData buildMockDirectoryData(boolean externalAccountRequired){
        Directory directory = new Directory();
        directory.setNewNonce("https://example.com/acme/new-nonce");
        directory.setNewAccount("https://example.com/acme/new-account");
        directory.setNewOrder("https://example.com/acme/new-order");
        directory.setNewAuthz("https://example.com/acme/new-authz");
        directory.setRevokeCert("https://example.com/acme/revoke-cert");
        directory.setKeyChange("https://example.com/acme/key-change");

        Meta meta = new Meta();
        meta.setExternalAccountRequired(externalAccountRequired);
        directory.setMeta(meta);

        DirectoryData directoryData = new DirectoryData(directory);
        directoryData.setName("acme-test");
        directoryData.setAllowPreAuthorization(false);
        return directoryData;
    }

    public static AccountData buildMockAccountData(){

        return accountData;
    }

    public static OrderData buildMockOrderData(StatusType statusType){
        Identifier identifier = new Identifier();
        identifier.setType(IdentifierType.DNS.toString());
        identifier.setValue(mockIdentifier);

        Order order = new Order();
        order.setStatus(statusType.toString());
        order.setIdentifiers(new Identifier[]{identifier});
        OrderData orderData = new OrderData(order, "acme-test", accountData.getId());

        return orderData;
    }

    public static MockCertificateAuthority buildMockCertificateAuthority(){
        return new MockCertificateAuthority();
    }

    public static ChallengeData buildMockChallengeData(StatusType statusType){
        Challenge challenge = new Challenge();
        challenge.setStatus(statusType.toString());
        challenge.setType(ChallengeType.HTTP.toString());
        ChallengeData challengeData = new ChallengeData(challenge, "acme-test");
        return challengeData;
    }

    public static AuthorizationData buildMockAuthorizationData(StatusType statusType){
        Authorization authorization = new Authorization();
        authorization.setStatus(statusType.toString());

        AuthorizationData authorizationData = new AuthorizationData(authorization, "acme-test");
        return authorizationData;
    }

}
