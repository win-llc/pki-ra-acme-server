package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.data.CertData;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class CertificatePersistence implements DataPersistence<CertData> {

    private Map<String, CertData> certDataMap = new HashMap<>();

    @Override
    public Optional<CertData> getById(String id) {
        return Optional.of(certDataMap.get(id));
    }

    @Override
    public CertData save(CertData data) {
        certDataMap.put(data.getId(), data);
        return data;
    }
}
