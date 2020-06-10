package com.winllc.acme.server.service.internal;

import com.winllc.acme.common.model.data.CertData;
import com.winllc.acme.server.MockUtils;
import com.winllc.acme.server.configuration.AppConfig;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.persistence.CertificatePersistence;
import com.winllc.acme.server.service.AbstractServiceTest;
import org.junit.Before;
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
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class SearchServiceTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private CertificatePersistence certificatePersistence;

    @BeforeEach
    public void before() throws CertificateException, IOException {
        CertData certData = MockUtils.buildMockCertData();

        when(certificatePersistence.findAllByAccountIdEquals(any())).thenReturn(Collections.singletonList(certData));
    }

    @Test
    public void findCertsAssociatedWithExternalAccount() throws Exception {
        mockMvc.perform(
                get("/search/findCertsAssociatedWithExternalAccount/eab1")
                        .contentType("application/json"))
                .andExpect(status().is(200))
        .andExpect(content().contentType("application/json"));
    }

    public void searchForCerts() {
        //todo
    }
}