package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.acme.Authorization;
import com.winllc.acme.server.model.data.AuthorizationData;
import com.winllc.acme.server.model.data.OrderData;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AuthorizationPersistence implements DataPersistence<AuthorizationData> {

    private Map<String, AuthorizationData> authorizationDataMap = new HashMap<>();

    @Override
    public Optional<AuthorizationData> getById(String id) {
        //TODO
        return Optional.of(authorizationDataMap.get(id));
    }

    @Override
    public AuthorizationData save(AuthorizationData data) {
        //TODO
        authorizationDataMap.put(data.getId(), data);
        return data;
    }

    public List<AuthorizationData>  getAllAuthorizationsForOrder(OrderData orderData){
        //TODO
        List<AuthorizationData> list = new ArrayList<>();
        for(AuthorizationData ad : authorizationDataMap.values()){
            if(ad.getOrderId() == orderData.getId()) list.add(ad);
        }

        return list;
    }
}
