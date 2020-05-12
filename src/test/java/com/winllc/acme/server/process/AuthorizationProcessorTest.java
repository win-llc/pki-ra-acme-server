package com.winllc.acme.server.process;

import com.winllc.acme.server.MockUtils;
import com.winllc.acme.server.configuration.AppConfig;
import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.model.acme.Authorization;
import com.winllc.acme.server.model.acme.Challenge;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.acme.Identifier;
import com.winllc.acme.server.model.data.AuthorizationData;
import com.winllc.acme.server.model.data.ChallengeData;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.persistence.AuthorizationPersistence;
import com.winllc.acme.server.persistence.ChallengePersistence;
import com.winllc.acme.server.service.internal.CertificateAuthorityService;
import com.winllc.acme.server.util.PayloadAndAccount;
import com.winllc.acme.server.util.SecurityValidatorUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/*
                   pending --------------------+
                      |                        |
    Challenge failure |                        |
           or         |                        |
          Error       |  Challenge valid       |
            +---------+---------+              |
            |                   |              |
            V                   V              |
         invalid              valid            |
                                |              |
                                |              |
                                |              |
                 +--------------+--------------+
                 |              |              |
                 |              |              |
          Server |       Client |   Time after |
          revoke |   deactivate |    "expires" |
                 V              V              V
              revoked      deactivated      expired
 */

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AppConfig.class)
@WebMvcTest(AuthorizationProcessor.class)
@TestPropertySource(locations="classpath:application.properties")
public class AuthorizationProcessorTest {

    @Autowired
    private AuthorizationPersistence authorizationPersistence;
    @MockBean
    private ChallengePersistence challengePersistence;
    @MockBean
    private CertificateAuthorityService certificateAuthorityService;
    @MockBean
    private ChallengeProcessor challengeProcessor;
    @Autowired
    private AuthorizationProcessor authorizationProcessor;

    private DirectoryData directoryData;

    @Before
    public void before(){
        directoryData = MockUtils.buildMockDirectoryData(false);
    }

    @Test
    public void buildNew(){
        AuthorizationData authorizationData = authorizationProcessor.buildNew(directoryData);

        assertNotNull(authorizationData.getId());
    }

    @Test
    public void buildCurrentAuthorization(){
        AuthorizationData authorizationData = authorizationProcessor.buildNew(directoryData);

        authorizationData = authorizationPersistence.save(authorizationData);

        List<ChallengeData> challengeDataList = new ArrayList<>();

        Challenge challenge = new Challenge();
        ChallengeData challengeData = new ChallengeData(challenge, directoryData.getName());
        challengeDataList.add(challengeData);

        when(challengePersistence.findAllByAuthorizationIdEquals(any())).thenReturn(challengeDataList);
        //when(authorizationPersistence.findById(any())).thenReturn(Optional.of(authorizationData));
        //when(authorizationPersistence.save(any())).thenReturn(authorizationData);

        assertNull(authorizationData.getObject().getChallenges());
        authorizationData = authorizationProcessor.buildCurrentAuthorization(authorizationData);

        assertEquals(authorizationData.getObject().getChallenges().length, 1);
    }

    @Test
    public void buildAuthorizationForIdentifier() throws AcmeServerException {
        ChallengeData challengeData = MockUtils.buildMockChallengeData(StatusType.PENDING);
        when(challengeProcessor.buildNew(any())).thenReturn(challengeData);
        when(challengePersistence.save(any())).thenReturn(challengeData);
        when(certificateAuthorityService.getByName(any())).thenReturn(MockUtils.buildMockCertificateAuthority());

        PayloadAndAccount<Identifier> payloadAndAccount = new PayloadAndAccount<>(MockUtils.identifier,
                MockUtils.buildMockAccountData(), directoryData);

        Optional<AuthorizationData> optionalAuthorizationData = authorizationProcessor.buildAuthorizationForIdentifier(MockUtils.identifier, payloadAndAccount,
                MockUtils.buildMockOrderData(StatusType.PENDING));

        assertTrue(optionalAuthorizationData.isPresent());
    }
}