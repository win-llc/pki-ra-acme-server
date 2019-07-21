package com.winllc.acme.server.model.data;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.acme.OrderList;
import com.winllc.acme.server.util.AppUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

//Section 7.1.2.1
public class OrderListData extends DataObject<OrderList> {
    private static int pageSize = 10;
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
        return buildBaseUrl() + "orders/" + getId();
    }

    //e.g. Link: <https://example.com/acme/orders/rzGoeA?cursor=2>;rel="next"
    public Optional<String> buildPaginatedLink(int currentRequestedPage){
        //TODO
        //only include rel=next if there are more pages
        if(getNumberOfPages() == currentRequestedPage){
            return Optional.empty();
        }

        return Optional.of("<" + buildUrl() + "?cursor=" + currentRequestedPage + 1 + ">;rel=\"next\"");
    }

    @Override
    public OrderList getObject() {
        return buildOrderList(Arrays.asList(getOrderIds()));
    }

    public OrderList buildOrderList(List<String> orderIds){
        List<String> urls = new ArrayList<>();
        for(String id : getOrderIds()){
            //TODO build url
            urls.add(id);
        }
        return null;
    }

    public int getNumberOfPages(){
        return AppUtil.getPages(Arrays.asList(getOrderIds()), pageSize).size();
    }

    public OrderList buildPaginatedOrderList(int page){
        List<List<String>> pages = AppUtil.getPages(Arrays.asList(getOrderIds()), pageSize);
        if(pages.size() < page){
            return buildOrderList(pages.get(page));
        }else{
            throw new ArrayIndexOutOfBoundsException("Bad page");
        }
    }

    public String[] getOrderIds() {
        if(orderIds == null) orderIds = new String[0];
        return orderIds;
    }

    public void setOrderIds(String[] orderIds) {
        this.orderIds = orderIds;
    }


}
