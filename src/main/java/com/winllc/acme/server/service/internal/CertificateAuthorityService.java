package com.winllc.acme.server.service.internal;

import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.acme.server.external.InternalCertAuthority;
import com.winllc.acme.server.model.data.DirectoryData;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@RestController
public class CertificateAuthorityService {

    //TODO CRUD service for CA's

    @PostConstruct
    private void postConstruct() {
        //TODO remove this

        CertificateAuthority ca = new InternalCertAuthority("ca1");
        save(ca);
    }

    private Map<String, CertificateAuthority> caMap = new HashMap<>();

    private void save(CertificateAuthority ca){
        caMap.put(ca.getName(), ca);
    }

    public CertificateAuthority getByName(String name){
        return caMap.get(name);
    }

    public CertificateAuthority getByDirectoryData(DirectoryData directoryData){
        return caMap.get(directoryData.getMapsToCertificateAuthorityName());
    }
}
