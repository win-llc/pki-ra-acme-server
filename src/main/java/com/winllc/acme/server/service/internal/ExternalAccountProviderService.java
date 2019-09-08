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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/externalAccountProvider")
public class ExternalAccountProviderService implements SettingsService<ExternalAccountProviderSettings, ExternalAccountProvider> {

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

        for(ExternalAccountProviderSettings providerSettings : persistence.findAll()){
            load(providerSettings);
        }

        //load(settings);
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
        log.info("Loading External Account Provider: "+settings.getName());
        ExternalAccountProvider accountProvider = new WINLLCExternalAccountProvider(settings);

        externalAccountProviderMap.put(accountProvider.getName(), accountProvider);
    }

    @GetMapping("/findSettingsByName/{name}")
    public ExternalAccountProviderSettings findSettingsByName(@PathVariable String name) {
        return persistence.findByName(name);
    }

    @DeleteMapping("/delete/{name}")
    public void delete(@PathVariable String name) {
        persistence.deleteByName(name);
    }

    @GetMapping("/findAllSettings")
    public List<ExternalAccountProviderSettings> findAllSettings() {
        return new ArrayList<>(persistence.findAll());
    }

    @GetMapping("/findAll")
    public List<ExternalAccountProvider> findAll() {
        return new ArrayList<>(externalAccountProviderMap.values());
    }
}
