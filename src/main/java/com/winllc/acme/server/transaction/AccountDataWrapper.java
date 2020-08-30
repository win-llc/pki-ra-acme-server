package com.winllc.acme.server.transaction;

import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.AuthorizationData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class AccountDataWrapper extends DataWrapper<AccountData> {

    AccountData accountData;

    public AccountDataWrapper(AccountData accountData, TransactionContext transactionContext) {
        super(transactionContext);
        this.accountData = accountData;
    }

    List<AuthorizationData> getPreAuthzList(){
        List<AuthorizationData> authorizationData = new ArrayList<>();
        for(String authzId : this.accountData.getPreAuthzIds()){
            Optional<AuthorizationData> optionalData = this.transactionContext.getAuthorizationPersistence().findById(authzId);
            if(optionalData.isPresent()) authorizationData.add(optionalData.get());
        }

        return authorizationData;
    }

    @Override
    void reloadChildren() {
        //no children
    }

    @Override
    AccountData getData() {
        return accountData;
    }

    @Override
    void persist() {
        this.accountData = transactionContext.getAccountPersistence().save(this.accountData);
    }
}
