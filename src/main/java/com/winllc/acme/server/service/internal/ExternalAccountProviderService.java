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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/externalAccountProvider")
public class ExternalAccountProviderService implements SettingsService<ExternalAccountProviderSettings, ExternalAccountProvider> {

    private static final Logger log = LogManager.getLogger(ExternalAccountProviderService.class);

    @Autowired
    private ExternalAccountProviderSettingsPersistence persistence;

    private Map<String, ExternalAccountProvider> externalAccountProviderMap;

    @PostConstruct
    private void postConstruct(){
        externalAccountProviderMap = new ConcurrentHashMap<>();

        for(ExternalAccountProviderSettings providerSettings : persistence.findAll()){
            try {
                load(providerSettings);
            }catch (Exception e){
                log.error("Coulc not load External Account Provider: "+providerSettings, e);
            }
        }
    }

    @GetMapping("/findByName/{name}")
    public ExternalAccountProvider findByName(@PathVariable String name){

        return externalAccountProviderMap.get(name);
    }

    @PostMapping("/save")
    public ExternalAccountProviderSettings save(@RequestBody ExternalAccountProviderSettings settings){
        settings = persistence.save(settings);

        load(settings);

        return settings;
    }



    public void load(ExternalAccountProviderSettings settings){
        log.info("Loading External Account Provider: "+settings.getName());
        ExternalAccountProvider accountProvider = new WINLLCExternalAccountProvider(settings);

        externalAccountProviderMap.put(accountProvider.getName(), accountProvider);
    }

    @GetMapping("/findSettingsByName/{name}")
    public ExternalAccountProviderSettings findSettingsByName(@PathVariable String name) {
        return persistence.findByName(name);
    }

    @GetMapping("/findSettingsById/{id}")
    public ExternalAccountProviderSettings findSettingsById(@PathVariable String id) throws Exception {
        Optional<ExternalAccountProviderSettings> settingsOptional = persistence.findById(id);
        if(settingsOptional.isPresent()){
            return settingsOptional.get();
        }
        throw new Exception("Could not find settings "+id);
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
