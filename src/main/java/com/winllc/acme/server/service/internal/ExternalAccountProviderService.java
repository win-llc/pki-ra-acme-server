package com.winllc.acme.server.service.internal;

import com.winllc.acme.server.external.ExternalAccountProvider;
import com.winllc.acme.server.external.ExternalAccountProviderImpl;
import com.winllc.acme.server.external.ExternalAccountProviderSettings;
import com.winllc.acme.server.model.data.DirectoryData;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@RestController
public class ExternalAccountProviderService {
    private Map<String, ExternalAccountProvider> externalAccountProviderMap = new HashMap<>();

    @PostConstruct
    private void postConstruct(){
        ExternalAccountProviderSettings settings = new ExternalAccountProviderSettings();
        settings.setName("daveCo");
        settings.setAccountVerificationUrl("http://localhost:8080/account/verify");
        settings.setLinkedDirectoryName("acme");

        loadExternalAccountProvider(settings);
    }

    public ExternalAccountProvider findByName(String name){
        //TODO

        return externalAccountProviderMap.get(name);
    }

    public void loadExternalAccountProvider(ExternalAccountProviderSettings settings){
        //TODO
        ExternalAccountProvider accountProvider = new ExternalAccountProviderImpl(settings);

        externalAccountProviderMap.put(accountProvider.getName(), accountProvider);
    }

}
