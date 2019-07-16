package com.winllc.acme.server;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.acme.server.external.CertificateAuthorityImpl;
import com.winllc.acme.server.external.ExternalAccountProvider;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.acme.Meta;
import com.winllc.acme.server.model.data.DirectoryData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Application {

    private static String hostname = "acme.winllc.com";
    public static String baseURL = "https://"+hostname+"/";

    public static List<ExternalAccountProvider> accountProviders = new ArrayList<>();
    public static List<String> usedNonces = new ArrayList<>();
    public static List<String> unUsedNonces = new ArrayList<>();

    public static DirectoryData directoryData;
    public static Map<String, DirectoryData> directoryDataMap;
    public static Directory directory;
    public static CertificateAuthority ca;
    public static Map<String, CertificateAuthority> availableCAs;

    public static RSAKey rsaJWK;

    static {
        availableCAs = new HashMap<>();
        directoryDataMap = new HashMap<>();
        ca = new CertificateAuthorityImpl("ca1");
        availableCAs.put(ca.getName(), ca);

        directory = new Directory();
        directory.setNewNonce(baseURL+"new-nonce");
        directory.setNewAccount(baseURL+"new-account");
        directory.setNewOrder(baseURL+"new-order");
        directory.setNewAuthz(baseURL+"new-authz");
        directory.setRevokeCert(baseURL+"revoke-cert");
        directory.setKeyChange(baseURL+"key-change");

        Meta meta = new Meta();
        meta.setTermsOfService(baseURL+"terms");
        meta.setWebsite(baseURL);
        meta.setCaaIdentities(new String[]{hostname});
        meta.setExternalAccountRequired(true);

        directory.setMeta(meta);

        directoryData = new DirectoryData(directory);
        directoryData.setAllowPreAuthorization(true);
        directoryData.setName("acme");
        directoryData.setMapsToCertificateAuthorityName(ca.getName());

        try {
            rsaJWK = new RSAKeyGenerator(2048)
                    .keyID("123")
                    .generate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
