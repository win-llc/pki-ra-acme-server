package com.winllc.acme.server.process;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.constants.ProblemType;
import com.winllc.acme.common.constants.StatusType;
import com.winllc.acme.common.model.AcmeJWSObject;
import com.winllc.acme.common.model.acme.Identifier;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.common.model.requestresponse.AccountRequest;
import com.winllc.acme.common.model.requestresponse.ExternalAccountBinding;
import com.winllc.acme.common.model.requestresponse.OrderRequest;
import com.winllc.acme.server.MockCertificateAuthority;
import com.winllc.acme.server.MockExternalAccountProvider;
import com.winllc.acme.server.MockUtils;
import com.winllc.acme.server.configuration.AppConfig;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.persistence.AccountPersistence;
import com.winllc.acme.server.persistence.internal.DirectoryDataSettingsPersistence;
import com.winllc.acme.server.service.AbstractServiceTest;
import com.winllc.acme.server.service.internal.CertificateAuthorityService;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import com.winllc.acme.server.service.internal.ExternalAccountProviderService;
import com.winllc.acme.server.transaction.AcmeTransactionManagement;
import com.winllc.acme.server.transaction.CertIssuanceTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

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
    @Autowired
    private AcmeTransactionManagement acmeTransactionManagement;
    @MockBean
    private CertificateAuthorityService certificateAuthorityService;

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
    public void deactivateAccount() throws Exception {
        when(certificateAuthorityService.getByName(any())).thenReturn(new MockCertificateAuthority());

        DirectoryData directoryData = directoryDataService.findByName("acme-test");
        AccountData accountData = MockUtils.buildMockAccountData();
        accountData.getObject().markValid();

        accountData = accountPersistence.save(accountData);

        Identifier identifier = new Identifier();
        identifier.setType("dns");
        identifier.setValue("test.winllc-dev.com");

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setIdentifiers(new Identifier[]{identifier});
        CertIssuanceTransaction certIssuanceTransaction = acmeTransactionManagement.startNewOrder(accountData, directoryData);
        certIssuanceTransaction.startOrder(orderRequest);

        //todo mock save returning wrong object, move
        accountData = accountProcessor.deactivateAccount(accountData, directoryData);

        assertEquals(accountData.getObject().getStatusType(), StatusType.DEACTIVATED);
    }
}