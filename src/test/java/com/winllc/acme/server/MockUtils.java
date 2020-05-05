package com.winllc.acme.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.winllc.acme.server.contants.ChallengeType;
import com.winllc.acme.server.contants.IdentifierType;
import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.model.acme.*;
import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.ChallengeData;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.model.data.OrderData;

public class MockUtils {

    public static final String mockIdentifier = "test.winllc.com";
    public static final Identifier identifier = new Identifier(IdentifierType.DNS, mockIdentifier);

    private static RSAKey rsaJWK;
    private static AccountData accountData;

    static {
        try {
            rsaJWK = new RSAKeyGenerator(2048)
                    .keyID("123")
                    .generate();
        } catch (JOSEException e) {
            e.printStackTrace();
        }

        Account account = new Account();
        account.setStatus(StatusType.VALID.toString());
        account.setOrders("http://localhost/acme-test/orders/1");
        accountData = new AccountData(account, "acme-test");
        accountData.setJwk(rsaJWK.toPublicJWK().toJSONString());
    }

    public static JWSObject buildCustomJwsObject(Object jsonObject, String url) throws JOSEException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return buildCustomJwsObject(objectMapper.writeValueAsString(jsonObject), url);
    }

    public static JWSObject buildCustomJwsObject(String jsonPayload, String url) throws JOSEException {

        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256)
                //.keyID(rsaJWK.getKeyID())
                .jwk(rsaJWK.toPublicJWK())
                .customParam("nonce", "1")
                .customParam("url", url)
                .build();

        Payload payload = new Payload(jsonPayload);
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);

        JWSSigner signer = new RSASSASigner(rsaJWK);
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

}
