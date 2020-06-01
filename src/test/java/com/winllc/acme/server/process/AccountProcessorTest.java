package com.winllc.acme.server.process;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.winllc.acme.server.MockExternalAccountProvider;
import com.winllc.acme.server.MockUtils;
import com.winllc.acme.server.configuration.AppConfig;
import com.winllc.acme.common.contants.ProblemType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.exceptions.InternalServerException;
import com.winllc.acme.server.external.ExternalAccountProvider;
import com.winllc.acme.common.model.AcmeJWSObject;
import com.winllc.acme.common.model.acme.OrderList;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.common.model.data.OrderListData;
import com.winllc.acme.common.model.requestresponse.AccountRequest;
import com.winllc.acme.common.model.requestresponse.ExternalAccountBinding;
import com.winllc.acme.server.persistence.AccountPersistence;
import com.winllc.acme.server.persistence.OrderListPersistence;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import com.winllc.acme.server.service.internal.ExternalAccountProviderService;
import net.minidev.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.ParseException;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/*
                  valid
                    |
                    |
        +-----------+-----------+
 Client |                Server |
deactiv.|                revoke |
        V                       V
   deactivated               revoked
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AppConfig.class)
@WebMvcTest(AccountProcessor.class)
public class AccountProcessorTest {

    @MockBean
    private AccountPersistence accountPersistence;
    @MockBean
    private OrderListPersistence orderListPersistence;
    @MockBean
    private DirectoryDataService directoryDataService;
    @MockBean
    private ExternalAccountProviderService externalAccountProviderService;
    @Autowired
    private AccountProcessor accountProcessor;

    @Before
    public void before(){
        OrderList orderList = new OrderList();
        OrderListData orderListData = new OrderListData(orderList, "directory");
        when(orderListPersistence.save(any())).thenReturn(orderListData);

        AccountData accountData = MockUtils.buildMockAccountData();
        when(accountPersistence.save(any())).thenReturn(accountData);

        ExternalAccountProvider eap = new MockExternalAccountProvider();
        when(externalAccountProviderService.findByName(any())).thenReturn(eap);
    }

    @Test
    public void request() throws Exception {
        DirectoryData directoryData = MockUtils.buildMockDirectoryData(false);
        when(directoryDataService.getByName(any())).thenReturn(Optional.of(directoryData));

        AccountRequest accountRequest = new AccountRequest();

        AcmeJWSObject acmeJWSObject = buildTestJwsObject(accountRequest);

        AccountData accountData = accountProcessor.processCreateNewAccount(accountRequest, directoryData, acmeJWSObject);

        assertEquals(directoryData.getName(), accountData.getDirectory());
    }

    @Test
    public void requestTosRequired() throws JsonProcessingException, JOSEException, ParseException {
        DirectoryData directoryData = MockUtils.buildMockDirectoryData(false);
        directoryData.getObject().getMeta().setTermsOfService("test");

        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setTermsOfServiceAgreed(false);

        AcmeJWSObject acmeJWSObject = MockUtils.buildCustomAcmeJwsObject(accountRequest, "");

        try {
            accountProcessor.processCreateNewAccount(accountRequest, directoryData, acmeJWSObject);
        } catch (AcmeServerException e) {
            e.printStackTrace();
            assertEquals(e.getProblemDetails().getType(), ProblemType.USER_ACTION_REQUIRED.toString());
        }

        try {
            accountRequest.setTermsOfServiceAgreed(true);
            AccountData accountData = accountProcessor.processCreateNewAccount(accountRequest, directoryData, acmeJWSObject);
            assertNotNull(accountData);
        } catch (AcmeServerException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void requestExternalAccountRequired() throws ParseException, JOSEException, JsonProcessingException {
        AccountData mockAccountData = MockUtils.buildMockAccountData();

        DirectoryData directoryData = MockUtils.buildMockDirectoryData(true);
        directoryData.getObject().getMeta().setTermsOfService("test");

        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setTermsOfServiceAgreed(true);

        JWSObject eabJws = MockUtils.buildCustomJwsObject(mockAccountData.getJwk(), "https://example.com/acme/new-account",
                "test", JWSAlgorithm.HS256, false);

        ExternalAccountBinding externalAccountBinding = new ExternalAccountBinding();

        externalAccountBinding.setPayload(eabJws.getPayload().toBase64URL().toString());
        externalAccountBinding.setProtectedProp(eabJws.getHeader().toBase64URL().toString());
        externalAccountBinding.setSignature(eabJws.getSignature().toString());

        accountRequest.setExternalAccountBinding(externalAccountBinding);

        AcmeJWSObject acmeJWSObject = MockUtils.buildCustomAcmeJwsObject(accountRequest, "https://example.com/acme/new-account");

        try {
            AccountData accountData = accountProcessor.processCreateNewAccount(accountRequest, directoryData, acmeJWSObject);
            assertNotNull(accountData);
        } catch (AcmeServerException e) {
            e.printStackTrace();
            fail();
        }
    }

    //@Test
    public void deactivateAccount() throws InternalServerException {
        AccountData accountData = MockUtils.buildMockAccountData();
        accountData.getObject().markValid();

        //todo mock save returning wrong object, move
        //accountData = accountProcessor.deactivateAccount(accountData);

        //assertEquals(accountData.getObject().getStatusType(), StatusType.INVALID);
    }

    private AcmeJWSObject buildTestJwsObject(Object obj) throws Exception{
        ObjectMapper objectMapper = new ObjectMapper();
        String accountRequestJson = objectMapper.writeValueAsString(obj);
        JSONObject jsonObject = JSONObjectUtils.parse(accountRequestJson);

        RSAKey rsaJwk = new RSAKeyGenerator(2048).generate();
        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .jwk(rsaJwk)
                .customParam("nonce", "test")
                .customParam("url", "http://acme.winllc.com/acme/new-account")
                .build();
        Payload payload = new Payload(jsonObject);

        JWSSigner signer = new RSASSASigner(rsaJwk);

        AcmeJWSObject acmeJWSObject = new AcmeJWSObject(jwsHeader, payload);
        acmeJWSObject.sign(signer);
        return acmeJWSObject;
    }
}