package com.winllc.acme.server.service.internal;

import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.acme.Meta;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.persistence.internal.DirectoryDataSettingsPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/directoryData")
public class DirectoryDataService implements SettingsService<DirectoryDataSettings, DirectoryData> {
    //TODO CRUD service for Directory Data

    @Autowired
    private DirectoryDataSettingsPersistence persistence;

    private Map<String, DirectoryData> directoryDataMap;

    @PostConstruct
    private void postConstruct(){
        directoryDataMap = new HashMap<>();
        //TODO remove this

        String directoryName = "acme";

        String directoryBaseUrl = Application.baseURL+directoryName+"/";
        Directory directory = new Directory();
        directory.setNewNonce(directoryBaseUrl+"new-nonce");
        directory.setNewAccount(directoryBaseUrl+"new-account");
        directory.setNewOrder(directoryBaseUrl+"new-order");
        directory.setNewAuthz(directoryBaseUrl+"new-authz");
        directory.setRevokeCert(directoryBaseUrl+"revoke-cert");
        directory.setKeyChange(directoryBaseUrl+"key-change");

        Meta meta = new Meta();
        meta.setTermsOfService(Application.baseURL+"acme");
        meta.setWebsite(Application.baseURL);
        meta.setCaaIdentities(new String[]{Application.hostname});
        meta.setExternalAccountRequired(true);

        directory.setMeta(meta);

        DirectoryData directoryData = new DirectoryData(directory);
        directoryData.setAllowPreAuthorization(true);
        directoryData.setName(directoryName);
        directoryData.setMapsToCertificateAuthorityName("ca1");
        directoryData.setExternalAccountProviderName("daveCo");
        directoryData.setTermsOfServiceLastUpdatedOn(Date.valueOf(LocalDate.now().minusMonths(1)));

        for(DirectoryDataSettings settings : persistence.findAll()){
            try {
                load(settings);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        //todo
        //load(directoryData);
    }


    @PostMapping("/save")
    public void save(@RequestBody DirectoryDataSettings settings) throws Exception {
        settings = persistence.save(settings);
        load(settings);
    }

    @GetMapping("/findSettingsByName/{name}")
    public DirectoryDataSettings findSettingsByName(@PathVariable String name) {
        return persistence.findByName(name);
    }

    @GetMapping("/findByName/{name}")
    public DirectoryData findByName(@PathVariable String name) {
        return directoryDataMap.get(name);
    }

    @DeleteMapping("/delete/{name}")
    public void delete(@PathVariable String name) {
        //todo
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
        DirectoryData directoryData = DirectoryData.buildFromSettings(settings);

        directoryDataMap.put(directoryData.getName(), directoryData);
    }
}
