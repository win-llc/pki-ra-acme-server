package com.winllc.acme.server.service.acme;

import com.nimbusds.jose.JWSObject;
import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.contants.IdentifierType;
import com.winllc.acme.common.contants.StatusType;
import com.winllc.acme.common.model.acme.Account;
import com.winllc.acme.common.model.acme.Identifier;
import com.winllc.acme.common.model.acme.Order;
import com.winllc.acme.common.model.acme.OrderList;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.common.model.data.OrderData;
import com.winllc.acme.common.model.data.OrderListData;
import com.winllc.acme.common.model.requestresponse.CertificateRequest;
import com.winllc.acme.common.model.requestresponse.OrderRequest;
import com.winllc.acme.server.MockCertificateAuthority;
import com.winllc.acme.server.MockUtils;
import com.winllc.acme.server.configuration.AppConfig;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.persistence.AccountPersistence;
import com.winllc.acme.server.persistence.OrderListPersistence;
import com.winllc.acme.server.persistence.OrderPersistence;
import com.winllc.acme.server.persistence.internal.DirectoryDataSettingsPersistence;
import com.winllc.acme.server.process.AccountProcessor;
import com.winllc.acme.server.process.OrderProcessor;
import com.winllc.acme.server.service.AbstractServiceTest;
import com.winllc.acme.server.service.internal.CertificateAuthorityService;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import com.winllc.acme.server.util.PayloadAndAccount;
import com.winllc.acme.server.util.SecurityValidatorUtil;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class OrderServiceTest extends AbstractServiceTest {

    private String decodedNewOrderProtected = "{\"alg\":\"RS256\",\"jwk\":{\"e\":\"AQAB\",\"kty\":\"RSA\",\"n\":\"w_RXzWeUTNCCqRFR_km9LHpxmYMgGLCj78G3PpH-1GGAKRPUihULrGQv5ti74AfOofSldGN9ALX-SKrrQXMCh227eIxF8FKRQGdEUjj8uiuAVI6wvrWMhLqKs_xuHx8qsyI893juC8LSdeuo_oFnxqLGB2YfJ6h7Ivb6XAll-7OXF7HWD9x6otAh9K4Pt1VZAxDnBxVOaa6sedAxFmP0a9cGD0QJbx-93xZBRi093m7VslIPZKbmI2x-kXISNxetGKWeR1XkZLIcz0thdkSkO40Pb5IS5A7xS8e14JoCmI6Mu3FnxokNnyAwCdqV8y7b-hV6EQ29P7VByTDj9o60fw\"},\"nonce\":\"MTU4ODAzMjk2NDQzMw==\",\"url\":\"http://localhost/acme-test/new-order\"}";
    private String decodedNewOrderPayload = "{\"identifiers\":[{\"type\":\"dns\",\"value\":\"ingress.kube.winllc-dev.com\"}]}";

    private static String newOrder = "{\n" +
            "   \"protected\":\"eyJhbGciOiJSUzI1NiIsImp3ayI6eyJlIjoiQVFBQiIsImt0eSI6IlJTQSIsIm4iOiJ3X1JYeldlVVROQ0NxUkZSX2ttOUxIcHhtWU1nR0xDajc4RzNQcEgtMUdHQUtSUFVpaFVMckdRdjV0aTc0QWZPb2ZTbGRHTjlBTFgtU0tyclFYTUNoMjI3ZUl4RjhGS1JRR2RFVWpqOHVpdUFWSTZ3dnJXTWhMcUtzX3h1SHg4cXN5STg5M2p1QzhMU2RldW9fb0ZueHFMR0IyWWZKNmg3SXZiNlhBbGwtN09YRjdIV0Q5eDZvdEFoOUs0UHQxVlpBeERuQnhWT2FhNnNlZEF4Rm1QMGE5Y0dEMFFKYngtOTN4WkJSaTA5M203VnNsSVBaS2JtSTJ4LWtYSVNOeGV0R0tXZVIxWGtaTEljejB0aGRrU2tPNDBQYjVJUzVBN3hTOGUxNEpvQ21JNk11M0ZueG9rTm55QXdDZHFWOHk3Yi1oVjZFUTI5UDdWQnlURGo5bzYwZncifSwibm9uY2UiOiJNVFU0T0RBek1qazJORFF6TXc9PSIsInVybCI6Imh0dHA6Ly8xOTIuMTY4LjEuMTM6ODE4MS9hY21lL25ldy1vcmRlciJ9\",\n" +
            "   \"payload\":\"eyJpZGVudGlmaWVycyI6W3sidHlwZSI6ImRucyIsInZhbHVlIjoiaW5ncmVzcy5rdWJlLndpbmxsYy1kZXYuY29tIn1dfQ\",\n" +
            "   \"signature\":\"iTPf4S7CGlQGWnWYDCzk2Tvh0sR8Kfwp8Q67X05EMGmu5lZ2xtCmuFT4oFVCF-c35wPsKfZRfWhStLYO5dxqMUUhhONiIpG-X6l62qzpbPLCccaQlxchdZq7wJ_fdACxSvMh3zY4wvUakV1rL7T_AMpXbKbV160lidwTTvU9uJ3fMS30yNtn7xbvqJlXf6RRNiWmWfKdaT2Ze3r74Qqfun-dtFdajSHubW5RXXGp5q1SM6ZtnybX1CEOpCbprC69yNm20M85N6nW6hnfWgHksIlNS1YKTXbMfMEEQbW_CgQGWQYL0YAK7lRRErMXLpM_eetZ2z7uNoHfyO1NAa0Asg\"\n" +
            "}";
    private static String newOrderFinalize = "{\n" +
            "   \"protected\":\"eyJhbGciOiJSUzI1NiIsImp3ayI6eyJlIjoiQVFBQiIsImt0eSI6IlJTQSIsIm4iOiJ3X1JYeldlVVROQ0NxUkZSX2ttOUxIcHhtWU1nR0xDajc4RzNQcEgtMUdHQUtSUFVpaFVMckdRdjV0aTc0QWZPb2ZTbGRHTjlBTFgtU0tyclFYTUNoMjI3ZUl4RjhGS1JRR2RFVWpqOHVpdUFWSTZ3dnJXTWhMcUtzX3h1SHg4cXN5STg5M2p1QzhMU2RldW9fb0ZueHFMR0IyWWZKNmg3SXZiNlhBbGwtN09YRjdIV0Q5eDZvdEFoOUs0UHQxVlpBeERuQnhWT2FhNnNlZEF4Rm1QMGE5Y0dEMFFKYngtOTN4WkJSaTA5M203VnNsSVBaS2JtSTJ4LWtYSVNOeGV0R0tXZVIxWGtaTEljejB0aGRrU2tPNDBQYjVJUzVBN3hTOGUxNEpvQ21JNk11M0ZueG9rTm55QXdDZHFWOHk3Yi1oVjZFUTI5UDdWQnlURGo5bzYwZncifSwibm9uY2UiOiJNVFU0T0RBek1qazROVGMyTXc9PSIsInVybCI6Imh0dHA6Ly8xOTIuMTY4LjEuMTM6ODE4MS9hY21lL29yZGVyL2IxZUZWZklkUDUvZmluYWxpemUifQ\",\n" +
            "   \"payload\":\"eyJjc3IiOiJNSUlDbFRDQ0FYMENBUUF3RnpFVk1CTUdBMVVFQ2hNTVkyVnlkQzF0WVc1aFoyVnlNSUlCSWpBTkJna3Foa2lHOXcwQkFRRUZBQU9DQVE4QU1JSUJDZ0tDQVFFQTRZcVZxT0JLQUZrTndTSlh4YXFxRzh1VXk5a0N3SnJYUWJUcDhwRERWMDJLVXVCdFpXbDB0ZnJtRVhQN2dWQW9DYzJSc1hidmlFcThzOGJfblVmWTdsVk5zekF3N3Jfcjhwdmhqc0tINFhOLVZvRW1CTkg1VlZqMkZvOVZLN2RpSGZkd3UySTRoMjdldTVnWHMtb2NNOEdoVGdSTUdoZVo4dkpJTzhUeV8yOUlFVnBwOEFSR3c5OU9RS0d0SUc5VU9aQmt2bTBZS2hXN1hNOXhqTnZTdkhhMV9tT2lVQVAwc05vaFJwMEdWbTBleEF4Z0pMY2kyLWxZZEc2Q1pZN2lua0J4ZlBUMXRiU3FKd1RzZjAxVXNRc1JtU0tKWWY2N01Edk9Ic01tRlQ3QlRiXzh4Q0pMTTRuaGYxeGZtOURFQzVYZWZuQjBIb0xZeHlfR0lERGFYd0lEQVFBQm9Ea3dOd1lKS29aSWh2Y05BUWtPTVNvd0tEQW1CZ05WSFJFRUh6QWRnaHRwYm1keVpYTnpMbXQxWW1VdWQybHViR3hqTFdSbGRpNWpiMjB3RFFZSktvWklodmNOQVFFTEJRQURnZ0VCQUpQeENINjJFUjRQVmZyN1ZfalBkeEFoMWs3S1ZNYi1lTVNhTHo2al9TMmd4d05kQWtkZldwSzdvYnc0Q2cxNHk4R1BZZkJORGJWdFFzVUVmN1ZEYk5hQ3BTUlViSWFDdWxIazlmNm5ITFpRNjlMcml1MjJidS1Ub2hJbjJNaXQydVJhbEtrd3c5X3ZXekNsZFc1SUNsVjc5TVlnMUJJSEtSUE9oVzZtdHp0TDZFS3d2bVJWUXh4eGJTem5pZHZSNWVuaXhLQnVtUF80dU5WQXljT0M0OGpoVXNyRUJTTDAwV2I4eTFxUDJmQkJFc3Y1SUFCckFibU5nc3NwRmRxUnVlbVpTMUp4N3NIWFp3Qm01bnloQ25FSUZnZ04wVHNtckJOem13emRpVWVsVDhMc1JEWnJtSmdOR3I4cXVxTFFNWTRBd25GT29rWGJPVEh6TVREVjB6TSJ9\",\n" +
            "   \"signature\":\"TmDaUa6aFMgghQHZFw94KKy5viO4_x5FmzZhQTOR1cm49vQ12Tmo1LTVMjkbA_tFV0Q_m_rJTVI-QCABEQSFhjIR4o565LYioM-tGCl9bM08hwTwilsmUw1fiK7T1zBSgGF3h4_-4c3nfjVKa4Mi-ggk50F4mwgJ3fA8gl1W0owkW4manfDiiT9gatNxN_bn16-jTjjTeBXb16Zs7JlbbOBf9UVIMzZkJx0UPUx1q0ycwsytSwr8NSdezyRCzRjSayP-Jl51Rw0ImmjkghfY-ESVkddDJDQco6qZp5xpVGLaA5FCz3xRdSoOFb24AD-EG7GY4XJ3oFsTboCVdCmmZA\"\n" +
            "}";

    private final String testCsr =
            "MIIDDDCCAfQCAQAwgZkxCzAJBgNVBAYTAlVTMREwDwYDVQQIDAhWaXJnaW5pYTET\n" +
            "MBEGA1UEBwwKQWxleGFuZHJpYTEQMA4GA1UECgwHV0lOIExMQzEMMAoGA1UECwwD\n" +
            "RGV2MSgwJgYJKoZIhvcNAQkBFhlwb3N0bWFzdGVyQHdpbmxsYy1kZXYuY29tMRgw\n" +
            "FgYDVQQDDA90ZXN0LndpbmxsYy5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAw\n" +
            "ggEKAoIBAQDL70P1WTSrj8qeC2tcVRVuPzLs7DwA6nkPaSk+2YHlQFsb5MpkMexQ\n" +
            "zpyDKPTXRAUmffBVOtMUeC9Npzdnu1K5bdWtjPMCoP24v/wJFTzx49KX/jdRrrYw\n" +
            "J68nMOvz3jyF0ISN3kmWXNE3rQk5mQ3NUKnBWU/j7T8UVBRdLPY1l2scLWfJINgm\n" +
            "bFDA1zg/KurEuTLfpJrKKOkNd2Nx1DstOMACtbMJOr7aQ0wPifWUCknF1Zo93osB\n" +
            "eqWr7qPUJnkEQrBkzM8qWNYo8JWd1A6e/5CMnL+0iJBxSswrJXXkVie3qSwZuhhH\n" +
            "AUAg2V9R82mtQgcDMzj2eTHWNsX5vhQHAgMBAAGgLTArBgkqhkiG9w0BCQ4xHjAc\n" +
            "MBoGA1UdEQQTMBGCD3Rlc3Qud2lubGxjLmNvbTANBgkqhkiG9w0BAQsFAAOCAQEA\n" +
            "jz/5pQk5kB798/5LN4rqgs02oic3rsKAQk99qW8ty+MWQh/Q4Jx5URW/RsFbjn64\n" +
            "fZ6rgVD491TNrse2ZE8/7iyjHEmn0vJyZ8aAVraxo445+lXcNYiluFvEdEG0v3qv\n" +
            "kUEry9++H5BXjx/EDwI7atY+1U9pmxKvzAoinBBrkxXsC49BY1+PNGRmfJPxznmN\n" +
            "poF6hkCJVX5Ygw6Ib4qdPAonbCiGM7yq6ur9V3K6HpOVcHEIErSCD4j4+mX//8JV\n" +
            "zkzJN+9CSiuL7eXJKoZbbYF/3EnlCKCFx+u//WfqbAsdBJL9s+FB7crUBdMgT0UY\n" +
            "RTJb2gZMJXwJ8vCPugoK9g";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DirectoryDataService directoryDataService;
    @Autowired
    private DirectoryDataSettingsPersistence directoryDataSettingsPersistence;
    @Autowired
    private AccountPersistence accountPersistence;
    @Autowired
    private OrderPersistence orderPersistence;
    @Autowired
    private OrderListPersistence orderListPersistence;
    @MockBean
    private CertificateAuthorityService certificateAuthorityService;

    @BeforeEach
    public void beforeEach() throws Exception {
        DirectoryDataSettings directoryDataSettings = new DirectoryDataSettings();
        directoryDataSettings.setName("acme-test");
        directoryDataSettings.setMetaExternalAccountRequired(true);
        directoryDataSettings.setAllowPreAuthorization(true);
        directoryDataSettings.setExternalAccountProviderName("test");
        directoryDataSettings = directoryDataSettingsPersistence.save(directoryDataSettings);
        directoryDataService.load(directoryDataSettings);

        AccountData accountData = MockUtils.buildMockAccountData();
        accountPersistence.save(accountData);
    }

    @AfterEach
    public void after(){
        directoryDataSettingsPersistence.deleteAll();
        accountPersistence.deleteAll();
    }

    //@Before
    public void before() throws AcmeServerException {
        //directoryData = MockUtils.buildMockDirectoryData(false);
        //accountData = MockUtils.buildMockAccountData();

        OrderRequest orderRequest = new OrderRequest();
        Identifier identifier = new Identifier();
        identifier.setType(IdentifierType.DNS.toString());
        identifier.setValue("test.winllc.com");
        orderRequest.setIdentifiers(new Identifier[]{identifier});

        OrderList orderList = new OrderList();
        orderList.setOrders(new String[]{"http://localhost/acme-test/order/1"});
        //OrderListData orderListData = new OrderListData(orderList, directoryData.getDirectory());

        Order order = new Order();
        order.setIdentifiers(new Identifier[]{identifier});
        //orderData = new OrderData(order, directoryData.getDirectory(), accountData.getId());

        //when(orderListPersistence.findById(any())).thenReturn(Optional.of(orderListData));
        //when(orderPersistence.save(any())).thenReturn(orderData);
    }

    @Test
    public void newOrder() throws Exception {
        when(certificateAuthorityService.getByName(any())).thenReturn(new MockCertificateAuthority());

        AccountData accountData = MockUtils.buildMockAccountData();
        //accountData = accountPersistence.save(accountData);

        OrderList orderList = new OrderList();
        OrderListData orderListData = new OrderListData(orderList, "acme-test");
        orderListData = orderListPersistence.save(orderListData);

        accountData.getObject().setOrders(orderListData.buildUrl("http://localhost/"));
        accountPersistence.save(accountData);

        OrderRequest orderRequest = new OrderRequest();
        Identifier identifier = new Identifier();
        identifier.setType(IdentifierType.DNS.toString());
        identifier.setValue("test.winllc.com");
        orderRequest.setIdentifiers(new Identifier[]{identifier});

        JWSObject jwsObject = MockUtils.buildCustomJwsObject(orderRequest, "http://localhost/acme-test/new-order");

        String json = MockUtils.jwsObjectAsString(jwsObject);

        mockMvc.perform(
                post("/acme-test/new-order")
                        .contentType("application/jose+json")
                        .content(json))
                .andExpect(status().is(201));
    }

    @Test
    public void getOrder() throws Exception {
        OrderData orderData = MockUtils.buildMockOrderData(StatusType.READY);
        orderData = orderPersistence.save(orderData);

        mockMvc.perform(
                post("/acme-test/order/"+orderData.getId())
                        .contentType("application/jose+json"))
                        //.content(json))
                .andExpect(status().is(200));
    }

    @Test
    public void finalizeOrder() throws Exception {
        CertificateRequest certificateRequest = new CertificateRequest();
        certificateRequest.setCsr(testCsr.replace("\n",""));

        OrderData orderData = MockUtils.buildMockOrderData(StatusType.READY);
        orderData = orderPersistence.save(orderData);

        JWSObject jwsObject = MockUtils.buildCustomJwsObject(certificateRequest,
                "http://localhost/acme-test/order/"+orderData.getId()+"/finalize");
        String json = MockUtils.jwsObjectAsString(jwsObject);

        mockMvc.perform(
                post("/acme-test/order/"+orderData.getId()+"/finalize")
                        .contentType("application/jose+json")
                .content(json))
                .andExpect(status().is(200));
    }

    @Test
    public void orderList() throws Exception {
        OrderList orderList = new OrderList();
        orderList.setOrders(new String[]{"http://localhost/acme-test/order/1"});
        OrderListData orderListData = new OrderListData(orderList, "acme-test");
        orderListData = orderListPersistence.save(orderListData);

        mockMvc.perform(
                get("/acme-test/orders/"+orderListData.getId())
                        .contentType("application/jose+json"))
                //.content(json))
                .andExpect(status().is(200));
    }
}