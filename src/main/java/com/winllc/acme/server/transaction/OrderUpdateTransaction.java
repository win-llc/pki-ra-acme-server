package com.winllc.acme.server.transaction;

import com.winllc.acme.common.contants.StatusType;
import com.winllc.acme.common.model.data.OrderData;

public class OrderUpdateTransaction extends AbstractTransaction {

    private OrderDataWrapper orderDataWrapper;

    OrderUpdateTransaction(TransactionContext transactionContext) {
        super(transactionContext);
    }

    public void updateOrderStatus(OrderData orderData, StatusType statusType) throws Exception {
        OrderDataWrapper orderDataWrapper = new OrderDataWrapper(orderData, transactionContext);
        orderDataWrapper.markInvalid();
        this.orderDataWrapper = orderDataWrapper;
    }

    public OrderDataWrapper getOrderDataWrapper() {
        return orderDataWrapper;
    }
}
