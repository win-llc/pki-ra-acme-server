package com.winllc.acme.server.service.internal;

import com.winllc.acme.common.AcmeCertAuthorityType;
import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.acme.server.external.InternalCertAuthority;
import com.winllc.acme.server.external.WINLLCCertAuthority;
import com.winllc.acme.server.persistence.internal.CertificateAuthoritySettingsPersistence;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/certAuthority")
public class CertificateAuthorityService implements SettingsService<CertificateAuthoritySettings, CertificateAuthority> {

    private static final Logger log = LogManager.getLogger(CertificateAuthorityService.class);

    @Autowired
    private CertificateAuthoritySettingsPersistence settingsPersistence;
    @Autowired
    private ExternalAccountProviderService externalAccountProviderService;

    private Map<String, CertificateAuthority> certificateAuthorityMap;

    @PostConstruct
    private void postConstruct() {
        certificateAuthorityMap = new ConcurrentHashMap<>();

        for(CertificateAuthoritySettings s : settingsPersistence.findAll()){
            try {
                load(s);
            } catch (Exception e) {
                log.error("Could not load "+s, e);
            }
        }

    }

    public CertificateAuthority load(CertificateAuthoritySettings settings) throws Exception {
        log.info("Loading Certificate Authority: "+settings.getName());

        CertificateAuthority ca = null;
        switch (settings.getType()){
            case "internal":
                ca = new InternalCertAuthority(settings);
                break;
            case "winllc":
                ca = new WINLLCCertAuthority(settings, externalAccountProviderService);
                break;
        }

        if(ca != null) {
            certificateAuthorityMap.put(ca.getName(), ca);
            return ca;
        }else{
            throw new Exception("Could not recognize CA from settings");
        }
    }

    @GetMapping("/acmeCertAuthorityTypes")
    public List<String> getCertTypes(){
        return Stream.of(AcmeCertAuthorityType.values())
                .map(v -> v.getValue())
                .collect(Collectors.toList());
    }

    public Map<String, List<String>> getCertAuthorityTypesAndRequiredProperties(){

        WINLLCCertAuthority.getRequiredProperties();

        //todo
        return null;
    }

    @PostMapping("/save")
    public CertificateAuthoritySettings save(@RequestBody CertificateAuthoritySettings settings) throws Exception {
        settings = settingsPersistence.save(settings);

        load(settings);

        return settings;
    }

    @GetMapping("/findSettingsByName/{name}")
    public CertificateAuthoritySettings findSettingsByName(@PathVariable String name) {
        return settingsPersistence.findByName(name);
    }

    @GetMapping("/findSettingsById/{id}")
    public CertificateAuthoritySettings findSettingsById(@PathVariable String id) throws Exception {
        Optional<CertificateAuthoritySettings> settingsOptional = settingsPersistence.findById(id);
        if(settingsOptional.isPresent()){
            return settingsOptional.get();
        }
        throw new Exception("Could not find settings "+id);
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

}
