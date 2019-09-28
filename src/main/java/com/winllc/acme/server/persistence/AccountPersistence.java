package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.data.AccountData;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AccountPersistence extends DataPersistence<AccountData> {

    Optional<AccountData> findByJwkEquals(String jwk);
    List<AccountData> findAllByEabKeyIdentifierEquals(String eabKeyIdentifier);
}
