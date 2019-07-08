package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.data.CertData;

import java.util.Optional;

public class CertificatePersistence implements DataPersistence<CertData> {
    @Override
    public Optional<CertData> getById(String id) {
        return Optional.empty();
    }

    @Override
    public CertData save(CertData data) {
        return null;
    }
}
