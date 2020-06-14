package com.winllc.acme.server.process;

import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.contants.StatusType;
import com.winllc.acme.common.model.acme.Challenge;
import com.winllc.acme.common.model.acme.Identifier;
import com.winllc.acme.common.model.data.AuthorizationData;
import com.winllc.acme.common.model.data.ChallengeData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.common.model.data.OrderData;
import com.winllc.acme.server.MockUtils;
import com.winllc.acme.server.configuration.AppConfig;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.exceptions.InternalServerException;
import com.winllc.acme.server.persistence.AuthorizationPersistence;
import com.winllc.acme.server.persistence.ChallengePersistence;
import com.winllc.acme.server.persistence.OrderPersistence;
import com.winllc.acme.server.persistence.internal.DirectoryDataSettingsPersistence;
import com.winllc.acme.server.service.AbstractServiceTest;
import com.winllc.acme.server.service.internal.CertificateAuthorityService;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import com.winllc.acme.server.util.PayloadAndAccount;
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

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestPropertySource(locations="classpath:application-test.properties")
public class AuthorizationProcessorTest extends AbstractServiceTest {

    @Autowired
    private AuthorizationPersistence authorizationPersistence;
    @MockBean
    private ChallengePersistence challengePersistence;
    @MockBean
    private CertificateAuthorityService certificateAuthorityService;
    @MockBean
    private ChallengeProcessor challengeProcessor;
    @MockBean
    private OrderPersistence orderPersistence;
    @Autowired
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
    public void buildNew() throws Exception {
        DirectoryData directoryData = directoryDataService.findByName("acme-test");
        AuthorizationData authorizationData = authorizationProcessor.buildNew(directoryData);

        assertNotNull(authorizationData.getId());
    }

    @Test
    public void buildCurrentAuthorization() throws Exception {
        DirectoryData directoryData = directoryDataService.findByName("acme-test");
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
    public void buildAuthorizationForIdentifier() throws Exception {
        DirectoryData directoryData = directoryDataService.findByName("acme-test");
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

    @Test
    public void challengeMarkedValid() throws InternalServerException {
        OrderData orderData = MockUtils.buildMockOrderData(StatusType.PENDING);
        when(orderPersistence.findById(orderData.getId())).thenReturn(Optional.of(orderData));

        AuthorizationData authorizationData = MockUtils.buildMockAuthorizationData(StatusType.PENDING);

        authorizationData.setOrderId(orderData.getId());
        authorizationData = authorizationPersistence.save(authorizationData);

        authorizationData = authorizationProcessor.challengeMarkedValid(authorizationData.getId());

        assertEquals(authorizationData.getObject().getStatusType(), StatusType.VALID);

        //cleanup
        authorizationPersistence.delete(authorizationData);
    }

    @Test
    public void getCurrentAuthorizationsForOrder(){
        AuthorizationData authorizationData = MockUtils.buildMockAuthorizationData(StatusType.PENDING);
        OrderData orderData = MockUtils.buildMockOrderData(StatusType.PENDING);

        authorizationData.setOrderId(orderData.getId());
        authorizationData = authorizationPersistence.save(authorizationData);

        List<AuthorizationData> currentAuthorizationsForOrder = authorizationProcessor.getCurrentAuthorizationsForOrder(orderData);
        assertEquals(currentAuthorizationsForOrder.size(), 1);

        //cleanup
        authorizationPersistence.delete(authorizationData);
    }
}