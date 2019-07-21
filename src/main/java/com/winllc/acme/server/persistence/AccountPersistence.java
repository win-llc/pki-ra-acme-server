package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.data.AccountData;

import java.util.Optional;

public class AccountPersistence {


    public Optional<AccountData> getByAccountId(String id){

        return Optional.empty();
    }

    public Optional<AccountData> getByJwk(String jwk){
        //TODO
        return Optional.empty();
    }

    public AccountData save(AccountData account){
        //TODO
        return null;
    }
}
