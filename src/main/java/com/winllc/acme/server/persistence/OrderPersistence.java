package com.winllc.acme.server.persistence;

import com.winllc.acme.common.model.data.OrderData;

import java.util.*;

public interface OrderPersistence extends DataPersistence<OrderData> {

    List<OrderData> findAllByAccountIdEquals(String accountId);
    Optional<OrderData> findDistinctByTransactionIdEquals(String id);

}
