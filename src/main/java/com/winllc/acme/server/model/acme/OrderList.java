package com.winllc.acme.server.model.acme;

public class OrderList {
    private String[] orders;

    public String[] getOrders() {
        if(orders == null) orders = new String[0];
        return orders;
    }

    public void setOrders(String[] orders) {
        this.orders = orders;
    }
}
