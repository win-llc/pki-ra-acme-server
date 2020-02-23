package com.winllc.acme.server.process;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.JWKGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.winllc.acme.common.ExternalAccountProviderSettings;
import com.winllc.acme.server.MockExternalAccountProvider;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.external.ExternalAccountProvider;
import com.winllc.acme.server.external.WINLLCExternalAccountProvider;
import com.winllc.acme.server.model.AcmeJWSObject;
import com.winllc.acme.server.model.acme.Account;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.acme.Meta;
import com.winllc.acme.server.model.acme.OrderList;
import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.model.data.OrderListData;
import com.winllc.acme.server.model.requestresponse.AccountRequest;
import com.winllc.acme.server.persistence.AccountPersistence;
import com.winllc.acme.server.persistence.OrderListPersistence;
import com.winllc.acme.server.persistence.OrderPersistence;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import com.winllc.acme.server.service.internal.ExternalAccountProviderService;
import net.minidev.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.matchers.Any;
import org.mockito.internal.matchers.Or;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;

import org.mockito.*;

import java.text.ParseException;

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
public class AccountProcessorTest {

    @Mock
    private AccountPersistence accountPersistence;
    @Mock
    private OrderPersistence orderPersistence;
    @Mock
    private OrderListPersistence orderListPersistence;
    @Mock
    private DirectoryDataService directoryDataService;
    @Mock
    private ExternalAccountProviderService externalAccountProviderService;
    @Autowired
    @InjectMocks
    private AccountProcessor accountProcessor;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void request() throws Exception {
        Meta meta = new Meta();
        meta.setExternalAccountRequired(false);
        Directory directory = new Directory();
        directory.setMeta(meta);
        DirectoryData directoryData = new DirectoryData(directory);
        directoryData.setName("directory");
        when(directoryDataService.findByName(any())).thenReturn(directoryData);

        OrderList orderList = new OrderList();
        OrderListData orderListData = new OrderListData(orderList, "directory");
        when(orderListPersistence.save(any())).thenReturn(orderListData);

        ExternalAccountProvider eap = new MockExternalAccountProvider();
        when(externalAccountProviderService.findByName(any())).thenReturn(eap);

        AccountData accountData = new AccountData(new Account(), directoryData.getName());
        when(accountPersistence.save(any())).thenReturn(accountData);

        AccountRequest accountRequest = new AccountRequest();

        AcmeJWSObject acmeJWSObject = buildTestJwsObject(accountRequest);

        accountData = accountProcessor.processCreateNewAccount(acmeJWSObject);

        assertEquals(directoryData.getName(), accountData.getDirectory());
    }

    public void requestTosRequired(){

    }

    public void requestExternalAccountRequired(){

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