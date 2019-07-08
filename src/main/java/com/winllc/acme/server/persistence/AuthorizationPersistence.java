package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.Authorization;
import com.winllc.acme.server.model.data.AuthorizationData;
import com.winllc.acme.server.model.data.OrderData;

import java.util.List;
import java.util.Optional;

public class AuthorizationPersistence implements DataPersistence<AuthorizationData> {
    @Override
    public Optional<AuthorizationData> getById(String id) {
        //TODO
        return Optional.empty();
    }

    @Override
    public AuthorizationData save(AuthorizationData data) {
        //TODO
        return null;
    }

    public List<AuthorizationData> getAllAuthorizationsForOrder(OrderData orderData){
        //TODO
        return null;
    }
}
