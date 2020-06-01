package com.winllc.acme.server.persistence;

import com.winllc.acme.common.model.data.AccountData;

import java.util.List;
import java.util.Optional;

public interface AccountPersistence extends DataPersistence<AccountData> {

    Optional<AccountData> findFirstByJwkEquals(String jwk);
    List<AccountData> findAllByEabKeyIdentifierEquals(String eabKeyIdentifier);
}
