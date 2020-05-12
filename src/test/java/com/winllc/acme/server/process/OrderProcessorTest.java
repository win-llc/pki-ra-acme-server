package com.winllc.acme.server.process;

import com.winllc.acme.server.MockUtils;
import com.winllc.acme.server.configuration.AppConfig;
import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.model.acme.Authorization;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.AuthorizationData;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.model.data.OrderData;
import com.winllc.acme.server.persistence.OrderPersistence;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AppConfig.class)
@WebMvcTest(OrderProcessor.class)
@TestPropertySource(locations="classpath:application.properties")
public class OrderProcessorTest {

    @Autowired
    private OrderProcessor orderProcessor;
    @MockBean
    private OrderPersistence orderPersistence;
    @MockBean
    private AuthorizationProcessor authorizationProcessor;

    @Test
    public void buildNew() {
        DirectoryData directoryData = MockUtils.buildMockDirectoryData(false);
        AccountData accountData = MockUtils.buildMockAccountData();
        try {
            orderProcessor.buildNew(directoryData);
            fail();
        }catch (UnsupportedOperationException e){
            assertTrue(true);
        }

        OrderData orderData = orderProcessor.buildNew(directoryData, accountData);
        assertEquals(orderData.getObject().getStatusType(), StatusType.PENDING);
    }

    @Test
    public void buildCurrentOrder() {
        AuthorizationData pendingAuthorizationData = MockUtils.buildMockAuthorizationData(StatusType.PENDING);

        DirectoryData directoryData = MockUtils.buildMockDirectoryData(false);
        AccountData accountData = MockUtils.buildMockAccountData();
        OrderData orderData = orderProcessor.buildNew(directoryData, accountData);

        when(orderPersistence.save(any())).thenReturn(orderData);
        when(authorizationProcessor.getCurrentAuthorizationsForOrder(any())).thenReturn(Collections.singletonList(pendingAuthorizationData));

        orderData = orderProcessor.buildCurrentOrder(orderData);

        assertEquals(orderData.getObject().getStatusType(), StatusType.PENDING);

        pendingAuthorizationData.getObject().markValid();

        when(authorizationProcessor.getCurrentAuthorizationsForOrder(any())).thenReturn(Collections.singletonList(pendingAuthorizationData));

        orderData = orderProcessor.buildCurrentOrder(orderData);

        assertEquals(orderData.getObject().getStatusType(), StatusType.READY);
    }

    @Test
    public void markInvalid() {
    }

    @Test
    public void authorizationMarkedValid() {
    }
}