package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.acme.Authorization;
import com.winllc.acme.server.model.data.AuthorizationData;
import com.winllc.acme.server.model.data.OrderData;
import org.springframework.stereotype.Component;

import java.util.*;

public interface AuthorizationPersistence extends DataPersistence<AuthorizationData> {

    List<AuthorizationData> findAllByOrderIdEquals(String orderId);

}
