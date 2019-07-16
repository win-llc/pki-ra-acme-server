package com.winllc.acme.server.process;

import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.exceptions.InternalServerException;
import com.winllc.acme.server.model.acme.Order;
import com.winllc.acme.server.model.data.AuthorizationData;
import com.winllc.acme.server.model.data.DataObject;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.model.data.OrderData;
import com.winllc.acme.server.persistence.AuthorizationPersistence;

import java.util.List;

/*
 pending --------------+
    |                  |
    | All authz        |
    | "valid"          |
    V                  |
  ready ---------------+
    |                  |
    | Receive          |
    | finalize         |
    | request          |
    V                  |
processing ------------+
    |                  |
    | Certificate      | Error or
    | issued           | Authorization failure
    V                  V
  valid             invalid
 */

public class OrderProcessor implements AcmeDataProcessor<OrderData> {

    private AuthorizationPersistence authorizationPersistence;

    @Override
    public OrderData buildNew(DirectoryData directoryData) {
        Order order = new Order();
        order.setStatus(StatusType.PENDING.toString());

        OrderData orderData = new OrderData(order, directoryData);

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


    public OrderData markInvalid(OrderData orderData) throws InternalServerException {
        StatusType statusType = orderData.getObject().getStatusType();
        if(statusType != StatusType.VALID){
            orderData.getObject().setStatus(StatusType.INVALID.toString());
            return orderData;
        }else{
            throw new InternalServerException("Order marked valid, can't set as invalid");
        }
    }
}
