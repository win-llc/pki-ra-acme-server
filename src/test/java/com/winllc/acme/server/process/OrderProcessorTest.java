package com.winllc.acme.server.process;

import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.contants.StatusType;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.AuthorizationData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.common.model.data.OrderData;
import com.winllc.acme.server.MockUtils;
import com.winllc.acme.server.configuration.AppConfig;
import com.winllc.acme.server.exceptions.InternalServerException;
import com.winllc.acme.server.persistence.OrderPersistence;
import com.winllc.acme.server.persistence.internal.DirectoryDataSettingsPersistence;
import com.winllc.acme.server.service.AbstractServiceTest;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestPropertySource(locations="classpath:application-test.properties")
public class OrderProcessorTest extends AbstractServiceTest {

    @Autowired
    private OrderProcessor orderProcessor;
    @Autowired
    private OrderPersistence orderPersistence;
    @MockBean
    private AuthorizationProcessor authorizationProcessor;

    @Autowired
    private DirectoryDataSettingsPersistence directoryDataSettingsPersistence;
    @Autowired
    private DirectoryDataService directoryDataService;

    @BeforeEach
    public void before() throws Exception {
        DirectoryDataSettings directoryDataSettings = new DirectoryDataSettings();
        directoryDataSettings.setName("acme-test");
        directoryDataSettings.setMetaExternalAccountRequired(true);
        directoryDataSettings.setExternalAccountProviderName("test");
        directoryDataSettings = directoryDataSettingsPersistence.save(directoryDataSettings);
        directoryDataService.load(directoryDataSettings);
    }

    @AfterEach
    public void after(){
        directoryDataSettingsPersistence.deleteAll();
    }

    @Test
    public void buildNew() throws Exception {
        DirectoryData directoryData = directoryDataService.findByName("acme-test");
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
    public void buildCurrentOrder() throws Exception {
        AuthorizationData pendingAuthorizationData = MockUtils.buildMockAuthorizationData(StatusType.PENDING);

        DirectoryData directoryData = directoryDataService.findByName("acme-test");
        AccountData accountData = MockUtils.buildMockAccountData();
        OrderData orderData = orderProcessor.buildNew(directoryData, accountData);

        //when(orderPersistence.save(any())).thenReturn(orderData);
        when(authorizationProcessor.getCurrentAuthorizationsForOrder(any())).thenReturn(Collections.singletonList(pendingAuthorizationData));

        orderData = orderProcessor.buildCurrentOrder(orderData);

        assertEquals(orderData.getObject().getStatusType(), StatusType.PENDING);

        pendingAuthorizationData.getObject().markValid();

        when(authorizationProcessor.getCurrentAuthorizationsForOrder(any())).thenReturn(Collections.singletonList(pendingAuthorizationData));

        orderData = orderProcessor.buildCurrentOrder(orderData);

        assertEquals(orderData.getObject().getStatusType(), StatusType.READY);

        //cleanup
        orderPersistence.delete(orderData);
    }

    @Test
    public void markInvalid() throws InternalServerException {
        //todo
        OrderData orderData = MockUtils.buildMockOrderData(StatusType.PENDING);
        orderData = orderPersistence.save(orderData);

        OrderData orderData1 = orderProcessor.markInvalid(orderData);
        assertEquals(StatusType.INVALID, orderData1.getObject().getStatusType());
    }

    @Test
    public void authorizationMarkedValid() throws InternalServerException {
        OrderData orderData = MockUtils.buildMockOrderData(StatusType.PENDING);
        orderData = orderPersistence.save(orderData);

        AuthorizationData pendingAuthorizationData = MockUtils.buildMockAuthorizationData(StatusType.PENDING);

        when(authorizationProcessor.getCurrentAuthorizationsForOrder(any())).thenReturn(Collections.singletonList(pendingAuthorizationData));

        Optional<OrderData> optionalOrderData = orderProcessor.authorizationMarkedValid(orderData.getId());

        if(optionalOrderData.isPresent()){
            assertEquals(optionalOrderData.get().getObject().getStatusType(), StatusType.PENDING);
        }else{
            fail();
        }

        pendingAuthorizationData.getObject().setStatus(StatusType.VALID.toString());

        optionalOrderData = orderProcessor.authorizationMarkedValid(orderData.getId());

        if(optionalOrderData.isPresent()){
            assertEquals(optionalOrderData.get().getObject().getStatusType(), StatusType.READY);
        }else{
            fail();
        }
    }
}