package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.OrderData;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public interface OrderPersistence extends DataPersistence<OrderData> {

    List<OrderData> findAllByAccountIdEquals(String accountId);

}
