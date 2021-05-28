package com.winllc.acme.server.service.internal;

import com.winllc.acme.common.ExternalAccountProviderSettings;
import com.winllc.acme.server.configuration.AccountProviderDefaultProperties;
import com.winllc.acme.server.external.ExternalAccountProvider;
import com.winllc.acme.server.external.WINLLCExternalAccountProvider;
import com.winllc.acme.server.persistence.internal.ExternalAccountProviderSettingsPersistence;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/externalAccountProvider")
public class ExternalAccountProviderService implements SettingsService<ExternalAccountProviderSettings, ExternalAccountProvider> {

    private static final Logger log = LogManager.getLogger(ExternalAccountProviderService.class);

    private final AccountProviderDefaultProperties defaultProperties;
    private final ExternalAccountProviderSettingsPersistence persistence;

    private Map<String, ExternalAccountProvider> externalAccountProviderMap;

    public ExternalAccountProviderService(ExternalAccountProviderSettingsPersistence persistence, AccountProviderDefaultProperties defaultProperties) {
        this.persistence = persistence;
        this.defaultProperties = defaultProperties;
    }

    @PostConstruct
    private void postConstruct(){
        externalAccountProviderMap = new ConcurrentHashMap<>();

        for(ExternalAccountProviderSettings providerSettings : findAllSettings()){
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

        Optional<ExternalAccountProviderSettings> defaultProvider = getDefaultProvider();

        if(defaultProvider.isPresent()){
            ExternalAccountProviderSettings defaultSettings = defaultProvider.get();

            //Ignore if update requested for default settings
            if(!defaultSettings.getName().equalsIgnoreCase(settings.getName())){
                settings = persistence.save(settings);
            }
        }else{
            settings = persistence.save(settings);
        }

        load(settings);

        return settings;
    }



    public ExternalAccountProvider load(ExternalAccountProviderSettings settings){
        log.info("Loading External Account Provider: "+settings.getName());
        //todo make more generic
        ExternalAccountProvider accountProvider = new WINLLCExternalAccountProvider(settings);

        externalAccountProviderMap.put(accountProvider.getName(), accountProvider);
        return accountProvider;
    }

    @GetMapping("/findSettingsByName/{name}")
    public ExternalAccountProviderSettings findSettingsByName(@PathVariable String name) {
        ExternalAccountProviderSettings settings = persistence.findByName(name);

        if(settings == null){
            Optional<ExternalAccountProviderSettings> defaultOptional = getDefaultProvider();
            if(defaultOptional.isPresent()){
                ExternalAccountProviderSettings temp = defaultOptional.get();
                if(temp.getName().equalsIgnoreCase(name)) settings = temp;
            }
        }

        return settings;
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
        List<ExternalAccountProviderSettings> list = new ArrayList<>(persistence.findAll());

        getDefaultProvider().ifPresent(p -> list.add(p));

        return list;
    }

    @GetMapping("/findAll")
    public List<ExternalAccountProvider> findAll() {
        return new ArrayList<>(externalAccountProviderMap.values());
    }

    private Optional<ExternalAccountProviderSettings> getDefaultProvider(){
        if(Objects.nonNull(defaultProperties.getBaseUrl()) && Objects.nonNull(defaultProperties.getName())){
            ExternalAccountProviderSettings settings = new ExternalAccountProviderSettings();
            settings.setBaseUrl(defaultProperties.getBaseUrl());
            settings.setName(defaultProperties.getName());

            return Optional.of(settings);
        }else{
            return Optional.empty();
        }
    }
}
