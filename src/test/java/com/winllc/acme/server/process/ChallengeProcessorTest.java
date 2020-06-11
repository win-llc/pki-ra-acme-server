package com.winllc.acme.server.process;

import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.contants.StatusType;
import com.winllc.acme.common.model.data.ChallengeData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.server.MockUtils;
import com.winllc.acme.server.configuration.AppConfig;
import com.winllc.acme.server.exceptions.InternalServerException;
import com.winllc.acme.server.persistence.ChallengePersistence;
import com.winllc.acme.server.persistence.internal.DirectoryDataSettingsPersistence;
import com.winllc.acme.server.service.AbstractServiceTest;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Base64;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestPropertySource(locations="classpath:application-test.properties")
public class ChallengeProcessorTest extends AbstractServiceTest {

    @Autowired
    private ChallengeProcessor challengeProcessor;
    @MockBean
    private ChallengePersistence challengePersistence;
    @MockBean
    private AuthorizationProcessor authorizationProcessor;

    @Autowired
    private DirectoryDataSettingsPersistence directoryDataSettingsPersistence;
    @Autowired
    private DirectoryDataService directoryDataService;

    @BeforeEach
    public void before() throws Exception {
        DirectoryDataSettings directoryDataSettings = new DirectoryDataSettings();
        directoryDataSettings.setName("acme-test");
        directoryDataSettings.setMetaExternalAccountRequired(true);
        directoryDataSettings.setExternalAccountProviderName("test");
        directoryDataSettings = directoryDataSettingsPersistence.save(directoryDataSettings);
        directoryDataService.load(directoryDataSettings);
    }

    @AfterEach
    public void after(){
        directoryDataSettingsPersistence.deleteAll();
    }

    @Test
    public void buildNew() {
        DirectoryData directoryData = directoryDataService.findByName("acme-test");
        ChallengeData challengeData = challengeProcessor.buildNew(directoryData);

        Base64.Decoder urlDecoder = java.util.Base64.getUrlDecoder();
        byte[] encoded = urlDecoder.decode(challengeData.getObject().getToken());

        assertEquals(new String(encoded).length(), 50);
    }

    @Test
    public void processing() throws InternalServerException {
        DirectoryData directoryData = directoryDataService.findByName("acme-test");
        ChallengeData challengeData = challengeProcessor.buildNew(directoryData);
        challengeData = challengeProcessor.processing(challengeData);

        assertEquals(challengeData.getObject().getStatus(), StatusType.PROCESSING.toString());

        challengeData.getObject().markValid();

        try {
            challengeProcessor.processing(challengeData);
            fail();
        }catch (InternalServerException e){
            assertTrue(true);
        }
    }

    @Test
    public void validation() throws InternalServerException {
        DirectoryData directoryData = MockUtils.buildMockDirectoryData(false);
        ChallengeData challengeData = challengeProcessor.buildNew(directoryData);
        challengeData = challengeProcessor.processing(challengeData);

        when(challengePersistence.save(any())).thenReturn(challengeData);
        when(authorizationProcessor.challengeMarkedValid(any())).thenReturn(null);

        challengeData = challengeProcessor.validation(challengeData, true, false);

        assertEquals(challengeData.getObject().getStatus(), StatusType.VALID.toString());
    }
}