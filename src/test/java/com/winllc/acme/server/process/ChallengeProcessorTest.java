package com.winllc.acme.server.process;

import com.winllc.acme.server.MockUtils;
import com.winllc.acme.server.configuration.AppConfig;
import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.exceptions.InternalServerException;
import com.winllc.acme.server.model.data.ChallengeData;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.persistence.ChallengePersistence;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Base64;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AppConfig.class)
@WebMvcTest(ChallengeProcessor.class)
@TestPropertySource(locations="classpath:application.properties")
public class ChallengeProcessorTest {

    @Autowired
    private ChallengeProcessor challengeProcessor;
    @MockBean
    private ChallengePersistence challengePersistence;
    @MockBean
    private AuthorizationProcessor authorizationProcessor;

    @Test
    public void buildNew() {
        DirectoryData directoryData = MockUtils.buildMockDirectoryData(false);
        ChallengeData challengeData = challengeProcessor.buildNew(directoryData);

        Base64.Decoder urlDecoder = java.util.Base64.getUrlDecoder();
        byte[] encoded = urlDecoder.decode(challengeData.getObject().getToken());

        assertEquals(new String(encoded).length(), 50);
    }

    @Test
    public void processing() throws InternalServerException {
        DirectoryData directoryData = MockUtils.buildMockDirectoryData(false);
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