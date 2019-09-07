package com.winllc.acme.server.service.internal;

import com.winllc.acme.common.ExternalAccountProviderSettings;
import com.winllc.acme.server.external.ExternalAccountProvider;
import com.winllc.acme.server.external.WINLLCExternalAccountProvider;
import com.winllc.acme.server.persistence.internal.ExternalAccountProviderSettingsPersistence;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/externalAccountProvider")
public class ExternalAccountProviderService {

    private static final Logger log = LogManager.getLogger(ExternalAccountProviderService.class);

    @Autowired
    private ExternalAccountProviderSettingsPersistence persistence;

    private Map<String, ExternalAccountProvider> externalAccountProviderMap = new HashMap<>();

    @PostConstruct
    private void postConstruct(){
        ExternalAccountProviderSettings settings = new ExternalAccountProviderSettings();
        settings.setName("daveCo");
        settings.setAccountVerificationUrl("http://localhost:8080/account/verify");
        settings.setLinkedDirectoryName("acme");

        load(settings);
    }

    @GetMapping("/findByName/{name}")
    public ExternalAccountProvider findByName(@PathVariable String name){
        //TODO

        return externalAccountProviderMap.get(name);
    }

    @PostMapping("/save")
    public void save(@RequestBody ExternalAccountProviderSettings settings){

        settings = persistence.save(settings);

        load(settings);
    }



    public void load(ExternalAccountProviderSettings settings){
        //TODO
        ExternalAccountProvider accountProvider = new WINLLCExternalAccountProvider(settings);

        externalAccountProviderMap.put(accountProvider.getName(), accountProvider);
    }

}
