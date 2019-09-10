package com.winllc.acme.server.process;

import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.exceptions.InternalServerException;
import com.winllc.acme.server.model.acme.Authorization;
import com.winllc.acme.server.model.acme.Order;
import com.winllc.acme.server.model.data.AuthorizationData;
import com.winllc.acme.server.model.data.DataObject;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.model.data.OrderData;
import com.winllc.acme.server.persistence.AuthorizationPersistence;
import com.winllc.acme.server.persistence.OrderPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

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
@Component
public class OrderProcessor implements AcmeDataProcessor<OrderData> {
    @Autowired
    private OrderPersistence orderPersistence;
    @Autowired
    private AuthorizationProcessor authorizationProcessor;

    @Override
    public OrderData buildNew(DirectoryData directoryData) {
        Order order = new Order();
        order.setStatus(StatusType.PENDING.toString());
        order.willExpireInMinutes(30);

        OrderData orderData = new OrderData(order, directoryData.getName());
        order.setFinalize(orderData.buildUrl()+"/finalize");

        return orderData;
    }

    //Section 7.1.6
    //If all of the authorization objects are valid, then set the order to ready
    public OrderData buildCurrentOrder(OrderData orderData){
        if(orderData.getObject().isExpired()){
            orderData.getObject().setStatus(StatusType.INVALID.toString());
            orderData = orderPersistence.save(orderData);
        }

        //Only check authorizations if currently in pending, per specified flow above
        if(orderData.getObject().getStatus().contentEquals(StatusType.PENDING.toString())) {
            boolean allInValidState = allAuthorizationsValidCheck(orderData);

            if (allInValidState) {
                orderData.getObject().setStatus(StatusType.READY.toString());
                orderData = orderPersistence.save(orderData);
            }
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

    //When an authorization is marked valid, update status of order if all authorizations are now valid
    public OrderData authorizationMarkedValid(String orderId) throws InternalServerException {
        Optional<OrderData> orderDataOptional = orderPersistence.findById(orderId);
        if(orderDataOptional.isPresent()) {
            OrderData orderData = orderDataOptional.get();
            boolean allInValidState = allAuthorizationsValidCheck(orderData);

            if (allInValidState) {
                orderData.getObject().setStatus(StatusType.READY.toString());
            }
            return orderData;
        }else{
            throw new InternalServerException("Could not find order "+orderId);
        }
    }

    private boolean allAuthorizationsValidCheck(OrderData orderData){
        List<AuthorizationData> authorizations = authorizationProcessor.getCurrentAuthorizationsForOrder(orderData);
        if(authorizations.size() == 0){
            //If not authorization, assume valid
            return true;
        }else {
            return authorizations.stream()
                    .map(DataObject::getObject)
                    .allMatch(a -> a.getStatus().contentEquals(StatusType.VALID.toString()));
        }
    }

}
