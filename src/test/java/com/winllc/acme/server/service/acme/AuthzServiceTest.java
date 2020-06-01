package com.winllc.acme.server.service.acme;

import com.nimbusds.jose.JWSObject;
import com.winllc.acme.server.MockUtils;
import com.winllc.acme.common.contants.ChallengeType;
import com.winllc.acme.common.contants.IdentifierType;
import com.winllc.acme.common.contants.StatusType;
import com.winllc.acme.common.model.AcmeJWSObject;
import com.winllc.acme.common.model.acme.Authorization;
import com.winllc.acme.common.model.acme.Challenge;
import com.winllc.acme.common.model.acme.Identifier;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.AuthorizationData;
import com.winllc.acme.common.model.data.ChallengeData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.server.persistence.AuthorizationPersistence;
import com.winllc.acme.server.persistence.ChallengePersistence;
import com.winllc.acme.server.process.AuthorizationProcessor;
import com.winllc.acme.server.service.AbstractServiceTest;
import com.winllc.acme.server.util.PayloadAndAccount;
import com.winllc.acme.server.util.SecurityValidatorUtil;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthzService.class)
public class AuthzServiceTest extends AbstractServiceTest {

    private static String authZ = "{\n" +
            "   \"protected\":\"eyJhbGciOiJSUzI1NiIsImp3ayI6eyJlIjoiQVFBQiIsImt0eSI6IlJTQSIsIm4iOiJ3X1JYeldlVVROQ0NxUkZSX2ttOUxIcHhtWU1nR0xDajc4RzNQcEgtMUdHQUtSUFVpaFVMckdRdjV0aTc0QWZPb2ZTbGRHTjlBTFgtU0tyclFYTUNoMjI3ZUl4RjhGS1JRR2RFVWpqOHVpdUFWSTZ3dnJXTWhMcUtzX3h1SHg4cXN5STg5M2p1QzhMU2RldW9fb0ZueHFMR0IyWWZKNmg3SXZiNlhBbGwtN09YRjdIV0Q5eDZvdEFoOUs0UHQxVlpBeERuQnhWT2FhNnNlZEF4Rm1QMGE5Y0dEMFFKYngtOTN4WkJSaTA5M203VnNsSVBaS2JtSTJ4LWtYSVNOeGV0R0tXZVIxWGtaTEljejB0aGRrU2tPNDBQYjVJUzVBN3hTOGUxNEpvQ21JNk11M0ZueG9rTm55QXdDZHFWOHk3Yi1oVjZFUTI5UDdWQnlURGo5bzYwZncifSwibm9uY2UiOiJNVFU0T0RBek1qazJORFk1TVE9PSIsInVybCI6Imh0dHA6Ly8xOTIuMTY4LjEuMTM6ODE4MS9hY21lL2F1dGh6L2VUZ2NHZzBZbWQifQ\",\n" +
            "   \"payload\":\"\",\n" +
            "   \"signature\":\"qwBaBiF7k76opdAOtTDIf0QudT25XTY72opsIQfF9_RVlqgUHTbA06DmwMLA_4qlIFxi05ZVcHbi6EkS1LfBj1PnSPvK-vB02V06cMBwrm8tMs5uTUz3uahPLyMN1aUS3WgPlZJvvQNXYmPpzrQEwC1Rlj3wDJjR9GiOqETYdjj1vjsH1s9mdS4LpDBRXe1acxXYyzWtXcJa_22X2Aeazu82MiHF_YXObbJ9PtV1qriyz74p4hoDYRDqzTqO6oS7NRTC7M5vPjirP4SBxIZLUoO7_TL0WM2pAt8pHdf4d7w0KsxeTIHsREZdJmOVblmlz5EVVOw67BAMBBNaPDPHHw\"\n" +
            "}";
    private static String chall = "{\n" +
            "   \"protected\":\"eyJhbGciOiJSUzI1NiIsImp3ayI6eyJlIjoiQVFBQiIsImt0eSI6IlJTQSIsIm4iOiJ3X1JYeldlVVROQ0NxUkZSX2ttOUxIcHhtWU1nR0xDajc4RzNQcEgtMUdHQUtSUFVpaFVMckdRdjV0aTc0QWZPb2ZTbGRHTjlBTFgtU0tyclFYTUNoMjI3ZUl4RjhGS1JRR2RFVWpqOHVpdUFWSTZ3dnJXTWhMcUtzX3h1SHg4cXN5STg5M2p1QzhMU2RldW9fb0ZueHFMR0IyWWZKNmg3SXZiNlhBbGwtN09YRjdIV0Q5eDZvdEFoOUs0UHQxVlpBeERuQnhWT2FhNnNlZEF4Rm1QMGE5Y0dEMFFKYngtOTN4WkJSaTA5M203VnNsSVBaS2JtSTJ4LWtYSVNOeGV0R0tXZVIxWGtaTEljejB0aGRrU2tPNDBQYjVJUzVBN3hTOGUxNEpvQ21JNk11M0ZueG9rTm55QXdDZHFWOHk3Yi1oVjZFUTI5UDdWQnlURGo5bzYwZncifSwibm9uY2UiOiJNVFU0T0RBek1qazJOVFF3Tnc9PSIsInVybCI6Imh0dHA6Ly8xOTIuMTY4LjEuMTM6ODE4MS9hY21lL2NoYWxsL1JxTlBRU2xZQmgifQ\",\n" +
            "   \"payload\":\"\",\n" +
            "   \"signature\":\"BfKmjbQGcJ8hTdImsDuiuXwt2KpeIqWP7WCrnZWKgLYbgvXFwiDk32lEaPZDGUQTAS491D0SidUkdygB94tRn_ydEBapVL9STxvQykiO39Oiz3_VZWvBcwEz_s9mvjV4MdCFi0uvH9tksBDZ-PiiizqI9p3QPL-w_UvA0u-UxmXxHkfkIODgfCv7YE2Sq3-_mVtsYCFHKAS-3LgZkTjmwnhTgPcBhRf6IorZ9Qew9RUv-XO7QU51Mvq9JAeLg1h0xiXZwUh5rpp9y__kgUntsCUngM02Se9Jkj2Pa-Ahsn-8_h5VlmrY6gaNzLN2FX7IdLrDzI7MIWy1EyRiRub1BQ\"\n" +
            "}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SecurityValidatorUtil securityValidatorUtil;
    @MockBean
    private AuthorizationPersistence authorizationPersistence;
    @MockBean
    private AuthorizationProcessor authorizationProcessor;
    @MockBean
    private ChallengePersistence challengePersistence;

    private DirectoryData directoryData;

    @Before
    public void before(){
        directoryData = MockUtils.buildMockDirectoryData(false);

        Challenge challenge = new Challenge();
        challenge.markPending();
        challenge.setType(ChallengeType.HTTP.toString());
        ChallengeData challengeData = new ChallengeData(challenge, directoryData.getName());

        Authorization authorization = new Authorization();
        Identifier identifier = new Identifier();
        identifier.setType(IdentifierType.DNS.toString());
        identifier.setValue(MockUtils.mockIdentifier);
        authorization.setIdentifier(identifier);
        authorization.setStatus(StatusType.PENDING.toString());

        AuthorizationData authorizationData = new AuthorizationData(authorization, directoryData.getName());

        when(authorizationPersistence.findById(any())).thenReturn(Optional.of(authorizationData));
        when(authorizationProcessor.buildCurrentAuthorization(any())).thenReturn(authorizationData);
        when(challengePersistence.findById(any())).thenReturn(Optional.of(challengeData));
    }


    @Test
    public void newAuthz() {
        //todo
    }

    @Test
    public void authz() throws Exception {
        AccountData accountData = MockUtils.buildMockAccountData();

        PayloadAndAccount<String> payloadAndAccount = new PayloadAndAccount<>("", accountData, directoryData);

        when(securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(any(AcmeJWSObject.class), any(), any(),
                isA(Class.class))).thenReturn(payloadAndAccount);

        JWSObject jwsObject = MockUtils.buildCustomJwsObject("", "http://localhost/acme-test/authz/1");
        String json = MockUtils.jwsObjectAsString(jwsObject);

        mockMvc.perform(
                post("/acme-test/authz/1")
                        .contentType("application/jose+json")
                        .content(json))
                .andExpect(status().is(200));
    }

    @Test
    public void challenge() throws Exception {

        mockMvc.perform(
                post("/acme-test/chall/1")
                        .contentType("application/jose+json"))
                        //.content(json))
                .andExpect(status().is(200));

    }
}