package com.winllc.acme.server.model.acme;

import java.util.Arrays;

public class OrderList {
    private String[] orders;

    public String[] getOrders() {
        if(orders == null) orders = new String[0];
        return orders;
    }

    public void setOrders(String[] orders) {
        this.orders = orders;
    }

    @Override
    public String toString() {
        return "OrderList{" +
                "orders=" + Arrays.toString(orders) +
                '}';
    }
}
