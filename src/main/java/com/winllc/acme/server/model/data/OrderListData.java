package com.winllc.acme.server.model.data;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.acme.OrderList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//Section 7.1.2.1
public class OrderListData extends DataObject<OrderList> {
    private String[] orderIds;

    public OrderListData(OrderList obj, DirectoryData directoryData) {
        super(obj, directoryData);
    }

    public void addOrder(OrderData order){
        List<String> list = Arrays.asList(getOrderIds());
        list.add(order.getId());
        setOrderIds(list.toArray(new String[0]));
    }

    @Override
    public String buildUrl() {
        return Application.baseURL + "orders/" + getId();
    }

    @Override
    public OrderList getObject() {
        return buildOrderList();
    }

    public OrderList buildOrderList(){
        List<String> urls = new ArrayList<>();
        for(String id : getOrderIds()){
            //TODO build url
            urls.add(id);
        }
        return null;
    }

    public String[] getOrderIds() {
        if(orderIds == null) orderIds = new String[0];
        return orderIds;
    }

    public void setOrderIds(String[] orderIds) {
        this.orderIds = orderIds;
    }
}
