package com.winllc.acme.server.persistence;

import com.winllc.acme.common.model.data.AuthorizationData;

import java.util.*;

public interface AuthorizationPersistence extends DataPersistence<AuthorizationData> {

    List<AuthorizationData> findAllByOrderIdEquals(String orderId);

}
