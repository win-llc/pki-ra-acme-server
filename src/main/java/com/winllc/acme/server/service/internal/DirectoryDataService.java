package com.winllc.acme.server.service.internal;

import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.persistence.internal.DirectoryDataSettingsPersistence;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/directoryData")
public class DirectoryDataService implements SettingsService<DirectoryDataSettings, DirectoryData> {

    private static final Logger log = LogManager.getLogger(DirectoryDataService.class);

    @Autowired
    private DirectoryDataSettingsPersistence persistence;

    private Map<String, DirectoryData> directoryDataMap;

    @PostConstruct
    private void postConstruct(){
        directoryDataMap = new ConcurrentHashMap<>();

        for(DirectoryDataSettings settings : persistence.findAll()){
            try {
                load(settings);
            }catch (Exception e){
                log.error("Could not load directory: "+settings, e);
            }
        }
    }


    @PostMapping("/save")
    public DirectoryDataSettings save(@RequestBody DirectoryDataSettings settings) throws Exception {
        settings = persistence.save(settings);
        load(settings);

        return settings;
    }

    @GetMapping("/findSettingsByName/{name}")
    public DirectoryDataSettings findSettingsByName(@PathVariable String name) {
        return persistence.findByName(name);
    }

    @GetMapping("/findSettingsById/{id}")
    public DirectoryDataSettings findSettingsById(@PathVariable String id) throws Exception {
        Optional<DirectoryDataSettings> settingsOptional = persistence.findById(id);
        if(settingsOptional.isPresent()){
            return settingsOptional.get();
        }
        throw new Exception("Could not find settings "+id);
    }

    @GetMapping("/findByName/{name}")
    public DirectoryData findByName(@PathVariable String name) {
        return directoryDataMap.get(name);
    }

    @DeleteMapping("/delete/{name}")
    public void delete(@PathVariable String name) {
        directoryDataMap.remove(name);

        persistence.deleteByName(name);
    }

    @GetMapping("/findAllSettings")
    public List<DirectoryDataSettings> findAllSettings() {
        return persistence.findAll();
    }

    @GetMapping("/findAll")
    public List<DirectoryData> findAll() {
        return new ArrayList<>(directoryDataMap.values());
    }

    @Override
    public void load(DirectoryDataSettings settings) throws Exception {
        log.info("Loading Directory Data: "+settings.getName());
        DirectoryData directoryData = DirectoryData.buildFromSettings(Application.baseURL, settings);

        directoryDataMap.put(directoryData.getName(), directoryData);
    }

    public Optional<DirectoryData> getByName(String name){
        if(directoryDataMap.containsKey(name)){
            return Optional.of(directoryDataMap.get(name));
        }else{
            return Optional.empty();
        }
    }
}
