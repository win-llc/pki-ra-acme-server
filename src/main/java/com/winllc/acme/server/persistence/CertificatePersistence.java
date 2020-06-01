package com.winllc.acme.server.persistence;

import com.winllc.acme.common.model.data.CertData;

import java.util.List;

public interface CertificatePersistence extends DataPersistence<CertData> {
    List<CertData> findAllByAccountIdEquals(String accountId);
}
