package com.winllc.acme.server.service.acme;

import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.server.configuration.AppConfig;
import com.winllc.acme.server.persistence.internal.DirectoryDataSettingsPersistence;
import com.winllc.acme.server.service.AbstractServiceTest;
import com.winllc.acme.server.service.internal.CertificateAuthorityService;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class DirectoryServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DirectoryDataService directoryDataService;
    @Autowired
    private DirectoryDataSettingsPersistence directoryDataSettingsPersistence;

    @BeforeEach
    public void before() throws Exception {
        DirectoryDataSettings directoryDataSettings = new DirectoryDataSettings();
        directoryDataSettings.setName("acme-test");
        directoryDataSettings.setMetaExternalAccountRequired(true);
        directoryDataSettings.setAllowPreAuthorization(true);
        directoryDataSettings.setExternalAccountProviderName("test");
        directoryDataSettings = directoryDataSettingsPersistence.save(directoryDataSettings);
        directoryDataService.load(directoryDataSettings);
    }

    @AfterEach
    public void after(){
        directoryDataSettingsPersistence.deleteAll();
    }

    @Test
    public void directory() throws Exception {

        mockMvc.perform(
                get("/acme-test/directory"))
                        //.contentType("application/jose+json"))
                //.content(json))
                .andExpect(status().is(200));

    }
}