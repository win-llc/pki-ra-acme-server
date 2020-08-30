package com.winllc.acme.server.transaction;

import com.winllc.acme.common.contants.StatusType;
import com.winllc.acme.common.model.acme.BaseAcmeObject;
import com.winllc.acme.common.model.data.DataObject;

abstract class DataWrapper<T extends DataObject> {
    TransactionContext transactionContext;

    public DataWrapper(TransactionContext transactionContext) {
        this.transactionContext = transactionContext;
    }

    abstract void reloadChildren();
    abstract T getData();
    StatusType getStatus(){
        Object obj = getData().getObject();
        if(obj instanceof BaseAcmeObject){
            return ((BaseAcmeObject<?>) obj).getStatusType();
        }else{
            throw new IllegalArgumentException("invalid data type, no status");
        }
    }

    void updateStatus(StatusType statusType){
        Object obj = getData().getObject();
        if(obj instanceof BaseAcmeObject){
            ((BaseAcmeObject<?>) obj).setStatus(statusType.toString());
            persist();
        }else{
            throw new IllegalArgumentException("invalid data type, no status");
        }
    }

    abstract void persist();
}