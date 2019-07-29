package com.winllc.acme.server;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.acme.server.external.InternalCertAuthority;
import com.winllc.acme.server.external.ExternalAccountProvider;
import com.winllc.acme.server.external.ExternalAccountProviderImpl;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.acme.Meta;
import com.winllc.acme.server.model.data.DirectoryData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Application {

    private static String hostname = "localhost:8181";
    public static String baseURL = "http://"+hostname+"/";
    public static String directoryName = "acme";

    public static List<String> usedNonces = new ArrayList<>();
    public static List<String> unUsedNonces = new ArrayList<>();

    public static Map<String, DirectoryData> directoryDataMap;
    public static Map<String, CertificateAuthority> availableCAs;
    public static Map<String, ExternalAccountProvider> externalAccountProviderMap;

    public static RSAKey rsaJWK;

    static {
        availableCAs = new HashMap<>();
        directoryDataMap = new HashMap<>();
        externalAccountProviderMap = new HashMap<>();
        CertificateAuthority ca = new InternalCertAuthority("ca1");
        availableCAs.put(ca.getName(), ca);

        String directoryBaseUrl = baseURL+directoryName+"/";
        Directory directory = new Directory();
        directory.setNewNonce(directoryBaseUrl+"new-nonce");
        directory.setNewAccount(directoryBaseUrl+"new-account");
        directory.setNewOrder(directoryBaseUrl+"new-order");
        directory.setNewAuthz(directoryBaseUrl+"new-authz");
        directory.setRevokeCert(directoryBaseUrl+"revoke-cert");
        directory.setKeyChange(directoryBaseUrl+"key-change");

        Meta meta = new Meta();
        meta.setTermsOfService(baseURL+"acme");
        meta.setWebsite(baseURL);
        meta.setCaaIdentities(new String[]{hostname});
        meta.setExternalAccountRequired(false);

        directory.setMeta(meta);

        DirectoryData directoryData = new DirectoryData(directory);
        directoryData.setAllowPreAuthorization(true);
        directoryData.setName(directoryName);
        directoryData.setMapsToCertificateAuthorityName(ca.getName());

        directoryDataMap.put(directoryData.getName(), directoryData);

        ExternalAccountProvider accountProvider = new ExternalAccountProviderImpl("daveCo", directoryData.getName(),
                "http://localhost:8080/account/verify");

        externalAccountProviderMap.put(accountProvider.getName(), accountProvider);

        try {
            rsaJWK = new RSAKeyGenerator(2048)
                    .keyID("123")
                    .generate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
