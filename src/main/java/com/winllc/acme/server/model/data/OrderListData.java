package com.winllc.acme.server.model.data;

import com.sun.org.apache.xpath.internal.operations.Or;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.acme.Order;
import com.winllc.acme.server.model.acme.OrderList;
import com.winllc.acme.server.util.AppUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

//Section 7.1.2.1
public class OrderListData extends DataObject<OrderList> {
    private static int pageSize = 10;

    public OrderListData(OrderList obj, DirectoryData directoryData) {
        super(obj, directoryData);
    }

    public void addOrder(OrderData order){
        List<String> list = Arrays.asList(getObject().getOrders());
        list.add(order.buildUrl());
        getObject().setOrders(list.toArray(new String[0]));
    }

    @Override
    public String buildUrl() {
        return buildBaseUrl() + "orders/" + getId();
    }

    //e.g. Link: <https://example.com/acme/orders/rzGoeA?cursor=2>;rel="next"
    public Optional<String> buildPaginatedLink(int currentRequestedPage){
        //only include rel=next if there are more pages
        if(getNumberOfPages() == currentRequestedPage){
            return Optional.empty();
        }

        return Optional.of("<" + buildUrl() + "?cursor=" + currentRequestedPage + 1 + ">;rel=\"next\"");
    }


    public int getNumberOfPages(){
        return AppUtil.getPages(Arrays.asList(getObject().getOrders()), pageSize).size();
    }

    public OrderList buildPaginatedOrderList(int page){
        String[] orders = getObject().getOrders();
        List<List<String>> pages = AppUtil.getPages(Arrays.asList(orders), pageSize);
        if(pages.size() < page){
            OrderList orderList = new OrderList();
            orderList.setOrders(pages.get(page).toArray(new String[0]));
            return orderList;
        }else{
            throw new ArrayIndexOutOfBoundsException("Bad page");
        }
    }


}
