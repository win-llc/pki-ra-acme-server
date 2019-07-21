package com.winllc.acme.server.service.internal;

import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.acme.server.model.data.DirectoryData;

import java.util.HashMap;
import java.util.Map;

public class CertificateAuthorityService {

    //TODO CRUD service for CA's

    private Map<String, CertificateAuthority> caMap = new HashMap<>();

    public CertificateAuthority getByName(String name){
        return caMap.get(name);
    }

    public CertificateAuthority getByDirectoryData(DirectoryData directoryData){
        return caMap.get(directoryData.getMapsToCertificateAuthorityName());
    }
}
