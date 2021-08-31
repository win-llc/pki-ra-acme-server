package com.winllc.acme.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.winllc.acme.common.constants.ChallengeType;
import com.winllc.acme.common.constants.IdentifierType;
import com.winllc.acme.common.constants.StatusType;
import com.winllc.acme.common.model.AcmeJWSObject;
import com.winllc.acme.common.model.acme.*;
import com.winllc.acme.common.model.data.*;
import com.winllc.acme.common.model.requestresponse.ExternalAccountBinding;
import com.winllc.acme.common.model.requestresponse.OrderRequest;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.acme.common.util.SecurityUtil;
import org.apache.commons.lang3.RandomStringUtils;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.UUID;

public class MockUtils {

    public static final String testX509Cert = "-----BEGIN CERTIFICATE-----\n" +
            "MIID1DCCArygAwIBAgIBJTANBgkqhkiG9w0BAQsFADBtMRMwEQYKCZImiZPyLGQB\n" +
            "GRYDY29tMRowGAYKCZImiZPyLGQBGRYKd2lubGxjLWRldjETMBEGCgmSJomT8ixk\n" +
            "ARkWA3BraTESMBAGCgmSJomT8ixkARkWAmNhMREwDwYDVQQDDAhXSU4gUk9PVDAe\n" +
            "Fw0yMDA0MjYxMzA4MjFaFw0yMjA0MTYxMzA4MjFaMCYxJDAiBgNVBAMMG2luZ3Jl\n" +
            "c3Mua3ViZS53aW5sbGMtZGV2LmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCC\n" +
            "AQoCggEBAMoI4iJlZAve6SZHBVL4mkmzLQ4NjyJTSx+tF9qAmh5/IP6c2bxqqKr2\n" +
            "aauNaYStU+7oWMxu7FMk/1atfWQ4ruZoSO9Eqx1sQqNr2obz23gHtAwWvOFxmmYQ\n" +
            "kvpJPPuuU8qUGpLKy1bQMLioptDLbnbbFcZcBJGbhdyJmyjxC9sOIOXqGrfBdpqw\n" +
            "dD6R3POL1CGchwO4C821x7ngGOqlfX/ysfuJsVsACYXiowHGvSBXsZt8gSb8EeFv\n" +
            "Kdcfziv4RGAqXB/jl4pF0WUcqXTo1ZdFtCi2KvLYOXD/Kdm2gMQ3GiCD7VfQb7bC\n" +
            "44vr4azxo0sC51sx6US/Jjidh+LbXqsCAwEAAaOBxTCBwjAfBgNVHSMEGDAWgBRG\n" +
            "EhS/MD8CvuMH3HIn1ytachRxuzAmBgNVHREEHzAdghtpbmdyZXNzLmt1YmUud2lu\n" +
            "bGxjLWRldi5jb20wSAYIKwYBBQUHAQEEPDA6MDgGCCsGAQUFBzABhixodHRwOi8v\n" +
            "ZG9ndGFnLWNhLndpbmxsYy1kZXYuY29tOjgwODAvY2Evb2NzcDAOBgNVHQ8BAf8E\n" +
            "BAMCBLAwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMA0GCSqGSIb3DQEB\n" +
            "CwUAA4IBAQBI5COMesYLsSxc2Tx54QtvzeNdecLqdboUpnVY842WcXCtI/CtvBhV\n" +
            "qKq4nCB7znpItuB7cgVn0Hxwtxr2w0wUfVtWxAklmj0Y3+sHFR3EG6zO3pbqPRT7\n" +
            "IBJvnvNLlmxMKy5zP1edn0DV/DFGJuBbMXOsVqw9xMNQj0IM9tIsjTT2tuU5AqVa\n" +
            "whrg05qNTuU3XRGc605eyzek0kXd6zrjaGS4YrN/9U533ncsEs1M+SIlpocvinRD\n" +
            "+2/vl1YfoDobxdSbWXYrgpxMBRYMbLcOwrXChT1v5FLYJqtpPEO4VkSQZkOy4vdR\n" +
            "JJjhv4LdCnyD/RT6lxXzMVzBqX5721Hu\n" +
            "-----END CERTIFICATE-----";

    public static final String testRootCa = "-----BEGIN CERTIFICATE-----\n" +
            "MIIEBDCCAuygAwIBAgIBATANBgkqhkiG9w0BAQsFADBtMRMwEQYKCZImiZPyLGQB\n" +
            "GRYDY29tMRowGAYKCZImiZPyLGQBGRYKd2lubGxjLWRldjETMBEGCgmSJomT8ixk\n" +
            "ARkWA3BraTESMBAGCgmSJomT8ixkARkWAmNhMREwDwYDVQQDDAhXSU4gUk9PVDAe\n" +
            "Fw0yMDA0MDQxODE3NTRaFw00MDA0MDQxODE3NTRaMG0xEzARBgoJkiaJk/IsZAEZ\n" +
            "FgNjb20xGjAYBgoJkiaJk/IsZAEZFgp3aW5sbGMtZGV2MRMwEQYKCZImiZPyLGQB\n" +
            "GRYDcGtpMRIwEAYKCZImiZPyLGQBGRYCY2ExETAPBgNVBAMMCFdJTiBST09UMIIB\n" +
            "IjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAt+B+eCFB412ZLI+Rkl9vRLLR\n" +
            "M3l/5xSUNeqj8rV6GAIv2JUSjD2by3o/52pncPJnc1iOCzxe79GXb1bXD6QJdNCJ\n" +
            "nDaUq585owtpBNFO+wl5cdtblmJaJJapuiM5xeis9E60ENulM3EKDanjtudWN62r\n" +
            "bGJxJ9m/LILt3x0wPad3Vsw/RA7bi66kxodBunioM9mc/4tlRE/GcVyYupfWYGh4\n" +
            "GHffH+q5HVHVn/NNpYWPtbddXgoihzuIOG6rIm2J+8nywO3i2zMZU7EFfWtUXPwn\n" +
            "ZqcGbp6UdbujctCYbcGP17KZgw6mbPnseS5kwlnkpjlvmZGdTS2D+MN0PR4aQQID\n" +
            "AQABo4GuMIGrMB8GA1UdIwQYMBaAFEYSFL8wPwK+4wfccifXK1pyFHG7MA8GA1Ud\n" +
            "EwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgHGMB0GA1UdDgQWBBRGEhS/MD8CvuMH\n" +
            "3HIn1ytachRxuzBIBggrBgEFBQcBAQQ8MDowOAYIKwYBBQUHMAGGLGh0dHA6Ly9k\n" +
            "b2d0YWctY2Eud2lubGxjLWRldi5jb206ODA4MC9jYS9vY3NwMA0GCSqGSIb3DQEB\n" +
            "CwUAA4IBAQCHUoQsQV3c+0tg7fL5E51HDB/sNJFQ/JPd93PJAq5KSWIdx3GjjkNb\n" +
            "bg2xonZz8x9A0M4WBODkTOX5DHrfnEK4I91yAezppKytKKGdx8258wVN1MV/kMdb\n" +
            "vGpWI4TrA/yzjZVOrDnJiBFPxGoep4ESnOEVP72oY92903KcytDKMbeFbTHSqZFl\n" +
            "O8t3TnqemWAK+q4CiNcRNKpLGRT2YPDFyKK1gIv0WSMnHSL4Nn0vIQnFEZgd/MIe\n" +
            "1iqwYSpQUEyzUMUUSVtb3aAGmxuPKN4p2hIpB+5KdU08vCt1W8kga+6szPb7umUg\n" +
            "w4cVuU8Kktg9dX8yDu4nr5KIh7s/Iog9\n" +
            "-----END CERTIFICATE-----";

    public static final String mockIdentifier = "test.winllc.com";
    public static final Identifier identifier = new Identifier(IdentifierType.DNS, mockIdentifier);

    public static JWK rsaJWK;
    public static JWK alternateRsaJwk;
    public static JWK hmacJwk;
    private static SecretKey hmacKey;

    static {
        try {
            rsaJWK = new RSAKeyGenerator(2048)
                    .keyID("123")
                    .generate();

            alternateRsaJwk = new RSAKeyGenerator(2048)
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
    }

    public static AcmeJWSObject buildCustomAcmeJwsObject(Object jsonObject, String url)
            throws JsonProcessingException, JOSEException, ParseException {
        JWSObject jwsObject = buildCustomJwsObject(jsonObject, url);
        String jsonString = MockUtils.jwsObjectAsString(jwsObject);
        return AcmeJWSObject.parse(jsonString);
    }

    public static AcmeJWSObject buildCustomAcmeJwsObject(Object jsonObject, String url, String kid)
            throws JsonProcessingException, JOSEException, ParseException {
        ObjectMapper objectMapper = new ObjectMapper();
        JWSObject jwsObject = buildCustomJwsObject(objectMapper.writeValueAsString(jsonObject), url, kid);
        String jsonString = MockUtils.jwsObjectAsString(jwsObject);
        return AcmeJWSObject.parse(jsonString);
    }

    public static AcmeJWSObject buildCustomAcmeJwsObjectWithAlternateJwk(Object jsonObject, String url, String kid)
            throws JsonProcessingException, JOSEException, ParseException {
        ObjectMapper objectMapper = new ObjectMapper();
        JWSObject jwsObject = buildCustomJwsObject(objectMapper.writeValueAsString(jsonObject), url, kid,
                JWSAlgorithm.RS256, false, alternateRsaJwk);
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

    public static JWSObject buildCustomJwsObject(String jsonPayload, String url, String kid,
                                                 JWSAlgorithm jwsAlgorithm, boolean hasNonce) throws JOSEException {
        return buildCustomJwsObject(jsonPayload, url, kid, jwsAlgorithm, hasNonce, null);
    }

    public static JWSObject buildCustomJwsObject(String jsonPayload, String url, String kid,
                                                 JWSAlgorithm jwsAlgorithm, boolean hasNonce, JWK jwk) throws JOSEException {

        JWSHeader.Builder builder;
        JWSSigner signer;

        if(JWSAlgorithm.Family.HMAC_SHA.contains(jwsAlgorithm)){
            builder = new JWSHeader.Builder(jwsAlgorithm)
                    .jwk(hmacJwk.toPublicJWK());
            signer = new MACSigner(hmacKey);
        }else{
            if(jwk != null) {
                builder = new JWSHeader.Builder(jwsAlgorithm)
                        .jwk(jwk.toPublicJWK());
                signer = new RSASSASigner((RSAKey) jwk);
            }else{
                builder = new JWSHeader.Builder(jwsAlgorithm)
                        .jwk(rsaJWK.toPublicJWK());
                signer = new RSASSASigner((RSAKey) rsaJWK);
            }
        }

        if(url != null){
            builder = builder.customParam("url", url);
        }

        if(hasNonce) {
            builder = builder.customParam("nonce", RandomStringUtils.random(10));
        }

        if(kid != null){
            builder = builder.keyID(kid);
        }

        JWSHeader jwsHeader = builder
                .build();

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
        directoryData.setAllowPreAuthorization(true);
        return directoryData;
    }

    public static AccountData buildMockAccountData(){
        Account account = new Account();
        account.setStatus(StatusType.VALID.toString());
        account.setOrders("http://localhost/acme-test/orders/1");
        account.setContact(new String[]{"mailto:test@test.com"});
        AccountData accountData = new AccountData(account, "acme-test");
        accountData.setJwk(rsaJWK.toPublicJWK().toJSONString());
        accountData.setEabKeyIdentifier("eab1");
        return accountData;
    }

    public static OrderData buildMockOrderData(StatusType statusType){
        AccountData accountData = buildMockAccountData();

        Identifier identifier = new Identifier();
        identifier.setType(IdentifierType.DNS.toString());
        identifier.setValue(mockIdentifier);

        Order order = new Order();
        order.setStatus(statusType.toString());
        order.setIdentifiers(new Identifier[]{identifier});
        OrderData orderData = new OrderData(order, "acme-test", accountData.getId());
        orderData.setTransactionId(UUID.randomUUID().toString());

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

    public static CertData buildMockCertData() throws CertificateException, IOException {
        AccountData accountData = MockUtils.buildMockAccountData();
        DirectoryData directoryData = MockUtils.buildMockDirectoryData(false);

        Certificate[] chains = CertUtil.trustChainStringToCertArray(testRootCa);
        X509Certificate cert = CertUtil.base64ToCert(testX509Cert);
        String[] base64Chain = CertUtil.certAndChainsToPemArray(cert, chains);

        CertData certData = new CertData(base64Chain, directoryData.getName(), accountData.getId());
        return certData;
    }

    public static ExternalAccountBinding buildMockExternalAccountBinding(String url) throws JOSEException {
        String testMacKey = SecurityUtil.generateRandomString(32);

        JWSHeader.Builder builder = new JWSHeader.Builder(JWSAlgorithm.HS256)
                .jwk(hmacJwk.toPublicJWK());
        builder.keyID("kidtest1");
        builder.customParam("url", url);

        Payload payload = new Payload(MockUtils.rsaJWK.toJSONObject());

        JWSSigner signer = new MACSigner(testMacKey);
        JWSObject testObj = new JWSObject(builder.build(), payload);
        testObj.sign(signer);

        ExternalAccountBinding eab = new ExternalAccountBinding(testObj);
        return eab;
    }

    public static OrderRequest buildMockOrderRequest(){
        OrderRequest orderRequest = new OrderRequest();
        Identifier identifier = new Identifier();
        identifier.setType(IdentifierType.DNS.toString());
        identifier.setValue("test.winllc.com");
        orderRequest.setIdentifiers(new Identifier[]{identifier});
        return orderRequest;
    }

}
