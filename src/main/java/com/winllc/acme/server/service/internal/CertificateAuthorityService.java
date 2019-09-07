package com.winllc.acme.server.service.internal;

import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.acme.server.external.InternalCertAuthority;
import com.winllc.acme.server.external.WINLLCCertAuthority;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.persistence.internal.CertificateAuthoritySettingsPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/certAuthority")
public class CertificateAuthorityService implements SettingsService<CertificateAuthoritySettings, CertificateAuthority> {

    //TODO CRUD service for CA's

    @Autowired
    private CertificateAuthoritySettingsPersistence settingsPersistence;

    private Map<String, CertificateAuthority> certificateAuthorityMap;

    @PostConstruct
    private void postConstruct() {
        certificateAuthorityMap = new HashMap<>();

        //TODO remove this
        CertificateAuthoritySettings settings = new CertificateAuthoritySettings();
        settings.setName("ca1");
        settings.setType("internal");

        try {
            load(settings);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load(CertificateAuthoritySettings settings) throws Exception {

        CertificateAuthority ca = null;
        switch (settings.getType()){
            case "internal":
                ca = new InternalCertAuthority(settings);
                break;
            case "winllc":
                ca = new WINLLCCertAuthority(settings);
                break;
        }

        if(ca != null) {
            certificateAuthorityMap.put(ca.getName(), ca);
        }else{
            throw new Exception("Could not recognize CA from settings");
        }
    }


    @PostMapping("/save")
    public void save(@RequestBody CertificateAuthoritySettings settings) throws Exception {
        //TODO persist settings

        settingsPersistence.save(settings);

        load(settings);
    }

    @GetMapping("/findSettingsByName/{name}")
    public CertificateAuthoritySettings findSettingsByName(@PathVariable String name) {
        return settingsPersistence.findByName(name);
    }

    @GetMapping("/findByName/{name}")
    public CertificateAuthority findByName(@PathVariable String name) {

        return certificateAuthorityMap.get(name);
    }

    @DeleteMapping("/delete/{name}")
    public void delete(@PathVariable String name) {
        CertificateAuthoritySettings settings = settingsPersistence.findByName(name);
        settingsPersistence.delete(settings);

        certificateAuthorityMap.remove(name);
    }

    @GetMapping("/findAllSettings")
    public List<CertificateAuthoritySettings> findAllSettings() {
        return settingsPersistence.findAll();
    }

    @GetMapping("/findAll")
    public List<CertificateAuthority> findAll() {
        return new ArrayList<>(certificateAuthorityMap.values());
    }

    @GetMapping("/getByName/{name}")
    public CertificateAuthority getByName(@PathVariable String name){
        return certificateAuthorityMap.get(name);
    }


    public CertificateAuthority getByDirectoryName(String directoryName){
        //todo
        //return caMap.get(directoryData.getMapsToCertificateAuthorityName());
        return null;
    }
}
