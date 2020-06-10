package com.winllc.acme.server.service.acme;

import com.nimbusds.jose.JWSObject;
import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.contants.RevocationReason;
import com.winllc.acme.common.model.AcmeJWSObject;
import com.winllc.acme.common.model.acme.Account;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.CertData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.common.model.requestresponse.RevokeCertRequest;
import com.winllc.acme.server.MockCertificateAuthority;
import com.winllc.acme.server.MockExternalAccountProvider;
import com.winllc.acme.server.MockUtils;
import com.winllc.acme.server.configuration.AppConfig;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.persistence.AccountPersistence;
import com.winllc.acme.server.persistence.CertificatePersistence;
import com.winllc.acme.server.persistence.internal.DirectoryDataSettingsPersistence;
import com.winllc.acme.server.service.AbstractServiceTest;
import com.winllc.acme.server.service.internal.CertificateAuthorityService;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import com.winllc.acme.server.service.internal.ExternalAccountProviderService;
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

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class CertServiceTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private CertificatePersistence certificatePersistence;
    @MockBean
    private ExternalAccountProviderService externalAccountProviderService;
    @MockBean
    private CertificateAuthorityService certificateAuthorityService;

    @Autowired
    private DirectoryDataSettingsPersistence directoryDataSettingsPersistence;
    @Autowired
    private DirectoryDataService directoryDataService;
    @Autowired
    private AccountPersistence accountPersistence;


    @BeforeEach
    public void before() throws Exception {
        DirectoryDataSettings directoryDataSettings = new DirectoryDataSettings();
        directoryDataSettings.setName("acme-test");
        directoryDataSettings.setMetaExternalAccountRequired(true);
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

    @Test
    public void certDownload() throws Exception {
        CertData certData = MockUtils.buildMockCertData();
        when(certificatePersistence.findById(any())).thenReturn(Optional.of(certData));

        JWSObject jwsObject = MockUtils.buildCustomJwsObject("", "http://localhost/acme-test/cert/1");
        String json = MockUtils.jwsObjectAsString(jwsObject);

        mockMvc.perform(
                post("/acme-test/cert/1")
                        .contentType("application/jose+json")
                        .content(json))
                .andExpect(status().is(200));
    }

    @Test
    public void certRevoke() throws Exception {
        RevokeCertRequest revokeCertRequest = new RevokeCertRequest();
        revokeCertRequest.setCertificate(MockUtils.testX509Cert);
        revokeCertRequest.setReason(RevocationReason.KEY_COMPROMISE.getCode());

        when(externalAccountProviderService.findByName(any())).thenReturn(new MockExternalAccountProvider());
        when(certificateAuthorityService.getByName(any())).thenReturn(new MockCertificateAuthority());

        AcmeJWSObject jwsObject = MockUtils.buildCustomAcmeJwsObject(revokeCertRequest, "http://localhost/acme-test/revoke-cert");
        String json = MockUtils.jwsObjectAsString(jwsObject);

        mockMvc.perform(
                post("/acme-test/revoke-cert")
                        .contentType("application/jose+json")
                        .content(json))
                .andExpect(status().is(200));
    }
}