package com.winllc.acme.server.process;

import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.model.Authorization;
import com.winllc.acme.server.model.Order;
import com.winllc.acme.server.model.data.AuthorizationData;
import com.winllc.acme.server.model.data.DataObject;
import com.winllc.acme.server.model.data.OrderData;
import com.winllc.acme.server.persistence.AuthorizationPersistence;

import java.util.List;

public class OrderProcessor implements AcmeDataProcessor<OrderData> {

    private AuthorizationPersistence authorizationPersistence;

    @Override
    public OrderData buildNew() {
        Order order = new Order();
        order.setStatus(StatusType.PENDING.toString());

        OrderData orderData = new OrderData(order);

        return orderData;
    }

    //Section 7.1.6
    //If all of the authorization objects are valid, then set the order to ready
    public OrderData buildCurrentOrder(OrderData orderData){
        List<AuthorizationData> authorizations = authorizationPersistence.getAllAuthorizationsForOrder(orderData);

        boolean allInValidState = authorizations.stream()
                .map(DataObject::getObject)
                .allMatch(a -> a.getStatus().contentEquals(StatusType.VALID.toString()));

        if(allInValidState){
            orderData.getObject().setStatus(StatusType.READY.toString());
        }

        return orderData;
    }
}
