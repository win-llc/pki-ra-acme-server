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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Application {

    public static String hostname = "192.168.1.13"+":8181";
    public static String baseURL = "http://"+hostname+"/";

    public static Map<String, ExternalAccountProvider> externalAccountProviderMap;

    static {
        externalAccountProviderMap = new HashMap<>();

        ExternalAccountProvider accountProvider = new ExternalAccountProviderImpl("daveCo", "acme",
                "http://localhost:8080/account/verify");

        externalAccountProviderMap.put(accountProvider.getName(), accountProvider);

    }
}
