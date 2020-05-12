package com.winllc.acme.server.service.internal;

import com.winllc.acme.common.util.CertUtil;
import com.winllc.acme.server.MockUtils;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.CertData;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.persistence.AccountPersistence;
import com.winllc.acme.server.persistence.CertificatePersistence;
import com.winllc.acme.server.service.AbstractServiceTest;
import com.winllc.acme.server.service.acme.AccountService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchService.class)
public class SearchServiceTest extends AbstractServiceTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private CertificatePersistence certificatePersistence;

    @Before
    public void before() throws AcmeServerException, CertificateException, IOException {

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