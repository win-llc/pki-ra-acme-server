package com.winllc.acme.server.process;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.contants.ProblemType;
import com.winllc.acme.common.contants.StatusType;
import com.winllc.acme.common.model.AcmeJWSObject;
import com.winllc.acme.common.model.acme.Directory;
import com.winllc.acme.common.model.acme.OrderList;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.common.model.data.OrderListData;
import com.winllc.acme.common.model.requestresponse.AccountRequest;
import com.winllc.acme.common.model.requestresponse.ExternalAccountBinding;
import com.winllc.acme.server.MockExternalAccountProvider;
import com.winllc.acme.server.MockUtils;
import com.winllc.acme.server.configuration.AppConfig;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.exceptions.InternalServerException;
import com.winllc.acme.server.external.ExternalAccountProvider;
import com.winllc.acme.server.persistence.AccountPersistence;
import com.winllc.acme.server.persistence.OrderListPersistence;
import com.winllc.acme.server.persistence.internal.DirectoryDataSettingsPersistence;
import com.winllc.acme.server.service.AbstractServiceTest;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import com.winllc.acme.server.service.internal.ExternalAccountProviderService;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import net.minidev.json.JSONObject;
import org.junit.Before;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.net.UnknownHostException;
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
@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class AccountProcessorTest extends AbstractServiceTest {

    @Autowired
    private DirectoryDataService directoryDataService;
    @MockBean
    private ExternalAccountProviderService externalAccountProviderService;
    @Autowired
    private AccountProcessor accountProcessor;
    @Autowired
    private AccountPersistence accountPersistence;
    @Autowired
    private DirectoryDataSettingsPersistence directoryDataSettingsPersistence;


    @BeforeEach
    public void before() throws Exception {
        when(externalAccountProviderService.findByName("test")).thenReturn(new MockExternalAccountProvider());

        DirectoryDataSettings directoryDataSettings = new DirectoryDataSettings();
        directoryDataSettings.setName("acme-test");
        directoryDataSettings.setMetaExternalAccountRequired(true);
        directoryDataSettings.setExternalAccountProviderName("test");
        directoryDataSettings = directoryDataSettingsPersistence.save(directoryDataSettings);
        directoryDataService.load(directoryDataSettings);

        DirectoryDataSettings directoryDataSettings2 = new DirectoryDataSettings();
        directoryDataSettings2.setName("acme-test-no-eab");
        directoryDataSettings.setMetaExternalAccountRequired(false);
        directoryDataSettings2 = directoryDataSettingsPersistence.save(directoryDataSettings2);
        directoryDataService.load(directoryDataSettings2);
    }

    @AfterEach
    public void after(){
        directoryDataSettingsPersistence.deleteAll();
        accountPersistence.deleteAll();
    }

    @Test
    public void request() throws Exception {
        DirectoryData directoryData = directoryDataService.findByName("acme-test");

        AccountData accountData = MockUtils.buildMockAccountData();
        ExternalAccountBinding eab = MockUtils.buildMockExternalAccountBinding("http://localhost/acme-test/new-account");

        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setContact(new String[]{"mailto:user@winllc-dev.com"});
        accountRequest.setTermsOfServiceAgreed(true);
        accountData.getObject().setContact(accountRequest.getContact());
        accountRequest.setExternalAccountBinding(eab);

        AcmeJWSObject acmeJWSObject = MockUtils.buildCustomAcmeJwsObject(accountRequest, "http://localhost/acme-test/new-account");

        accountData = accountProcessor.processCreateNewAccount(accountRequest, directoryData, acmeJWSObject);

        assertEquals(directoryData.getName(), accountData.getDirectory());
    }

    @Test
    public void requestTosRequired() throws Exception {
        DirectoryData directoryData = directoryDataService.findByName("acme-test-no-eab");
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
    public void requestExternalAccountRequired() throws Exception {
        AccountData mockAccountData = MockUtils.buildMockAccountData();

        DirectoryData directoryData = directoryDataService.findByName("acme-test");
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

    @Test
    public void deactivateAccount() throws InternalServerException {
        AccountData accountData = MockUtils.buildMockAccountData();
        accountData.getObject().markValid();

        //todo mock save returning wrong object, move
        accountData = accountProcessor.deactivateAccount(accountData);

        assertEquals(accountData.getObject().getStatusType(), StatusType.DEACTIVATED);
    }
}