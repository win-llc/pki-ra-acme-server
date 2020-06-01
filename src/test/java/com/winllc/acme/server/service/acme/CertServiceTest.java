package com.winllc.acme.server.service.acme;

import com.nimbusds.jose.JWSObject;
import com.winllc.acme.server.MockExternalAccountProvider;
import com.winllc.acme.server.MockUtils;
import com.winllc.acme.common.contants.RevocationReason;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.common.model.AcmeJWSObject;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.CertData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.common.model.requestresponse.RevokeCertRequest;
import com.winllc.acme.server.persistence.CertificatePersistence;
import com.winllc.acme.server.service.AbstractServiceTest;
import com.winllc.acme.server.service.internal.ExternalAccountProviderService;
import com.winllc.acme.server.util.PayloadAndAccount;
import com.winllc.acme.server.util.SecurityValidatorUtil;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CertService.class)
public class CertServiceTest extends AbstractServiceTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private CertificatePersistence certificatePersistence;
    @MockBean
    private SecurityValidatorUtil securityValidatorUtil;
    @MockBean
    private ExternalAccountProviderService externalAccountProviderService;

    @Before
    public void before() throws CertificateException, IOException, AcmeServerException {
        AccountData accountData = MockUtils.buildMockAccountData();
        DirectoryData directoryData = MockUtils.buildMockDirectoryData(false);

        CertData certData = MockUtils.buildMockCertData();
        when(certificatePersistence.findById(any())).thenReturn(Optional.of(certData));

        PayloadAndAccount<String> payloadAndAccount = new PayloadAndAccount<>("", accountData, directoryData);
        when(securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(any(), any(Class.class))).thenReturn(payloadAndAccount);
    }

    @Test
    public void certDownload() throws Exception {
        mockMvc.perform(
                post("/acme-test/cert/1")
                        .contentType("application/jose+json")
                        .content(""))
                .andExpect(status().is(200));
    }

    @Test
    public void certRevoke() throws Exception {
        AccountData accountData = MockUtils.buildMockAccountData();
        DirectoryData directoryData = MockUtils.buildMockDirectoryData(false);

        RevokeCertRequest revokeCertRequest = new RevokeCertRequest();
        revokeCertRequest.setCertificate(MockUtils.testX509Cert);
        revokeCertRequest.setReason(RevocationReason.KEY_COMPROMISE.getCode());

        PayloadAndAccount<RevokeCertRequest> payloadAndAccount = new PayloadAndAccount<>(revokeCertRequest, accountData, directoryData);
        when(securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(any(AcmeJWSObject.class), any(),
                isA(Class.class))).thenReturn(payloadAndAccount);

        when(externalAccountProviderService.findByName(any())).thenReturn(new MockExternalAccountProvider());

        JWSObject jwsObject = MockUtils.buildCustomJwsObject(revokeCertRequest, "http://localhost/acme-test/revoke-cert");
        String json = MockUtils.jwsObjectAsString(jwsObject);

        mockMvc.perform(
                post("/acme-test/revoke-cert")
                        .contentType("application/jose+json")
                        .content(json))
                .andExpect(status().is(200));
    }
}