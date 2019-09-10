package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.data.AccountData;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface AccountPersistence extends DataPersistence<AccountData> {

    Optional<AccountData> findByJwkEquals(String jwk);

    /*
    private Map<String, AccountData> accountMap = new HashMap<>();

    public Optional<AccountData> getByAccountId(String id){
        //TODO
        return Optional.of(accountMap.get(id));
    }

    public Optional<AccountData> getByJwk(String jwk){
        //TODO
        for(AccountData data : accountMap.values()){
            if(data.getJwk().contentEquals(jwk)) return Optional.of(data);
        }

        return Optional.empty();
    }

    public AccountData save(AccountData account){
        accountMap.put(account.getId(), account);
        return account;
    }

     */
}
