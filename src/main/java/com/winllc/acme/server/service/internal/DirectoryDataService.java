package com.winllc.acme.server.service.internal;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.acme.Meta;
import com.winllc.acme.server.model.data.DirectoryData;
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
@RequestMapping("/admin/directory")
public class DirectoryDataService {
    //TODO CRUD service for Directory Data

    private Map<String, DirectoryData> directoryDataMap = new HashMap<>();

    @PostConstruct
    private void postConstruct(){
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

        addDirectory(directoryData);
    }


    @GetMapping
    public List<DirectoryData> getAll(){
        //TODO
        return new ArrayList<>(directoryDataMap.values());
    }

    @GetMapping("/byName/{name}")
    public DirectoryData getByName(String name){
        //TODO
        return directoryDataMap.get(name);
    }

    @PostMapping("/add")
    public DirectoryData addDirectory(DirectoryData directoryData){
        //TODO
        directoryDataMap.put(directoryData.getName(), directoryData);
        return directoryData;
    }

    @PostMapping("/update")
    public DirectoryData updateDirectory(DirectoryData directoryData){
        //TODO
        return addDirectory(directoryData);
    }

    @GetMapping("/delete/{name}")
    public void deleteDirectory(@PathVariable String name){
        //TODO
        directoryDataMap.remove(name);
    }
}
