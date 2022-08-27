package com.winllc.acme.server.service.acme;

import com.nimbusds.jose.JWSObject;
import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.model.AcmeJWSObject;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.common.model.requestresponse.AccountRequest;
import com.winllc.acme.common.model.requestresponse.ExternalAccountBinding;
import com.winllc.acme.common.model.requestresponse.KeyChangeRequest;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.MockExternalAccountProvider;
import com.winllc.acme.server.MockUtils;
import com.winllc.acme.server.TestConfig;
import com.winllc.acme.server.configuration.AppConfig;
import com.winllc.acme.server.persistence.AccountPersistence;
import com.winllc.acme.server.persistence.internal.DirectoryDataSettingsPersistence;
import com.winllc.acme.server.service.AbstractServiceTest;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import com.winllc.acme.server.service.internal.ExternalAccountProviderService;
import com.winllc.acme.server.util.PayloadAndAccount;
import com.winllc.acme.server.util.SecurityValidatorUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



public class AccountServiceTest extends AbstractServiceTest {

    private static String newAccountRequest = "{\n" +
            "   \"protected\":\"eyJhbGciOiJSUzI1NiIsImp3ayI6eyJlIjoiQVFBQiIsImt0eSI6IlJTQSIsIm4iOiJ3X1JYeldlVVROQ0NxUkZSX2ttOUxIcHhtWU1nR0xDajc4RzNQcEgtMUdHQUtSUFVpaFVMckdRdjV0aTc0QWZPb2ZTbGRHTjlBTFgtU0tyclFYTUNoMjI3ZUl4RjhGS1JRR2RFVWpqOHVpdUFWSTZ3dnJXTWhMcUtzX3h1SHg4cXN5STg5M2p1QzhMU2RldW9fb0ZueHFMR0IyWWZKNmg3SXZiNlhBbGwtN09YRjdIV0Q5eDZvdEFoOUs0UHQxVlpBeERuQnhWT2FhNnNlZEF4Rm1QMGE5Y0dEMFFKYngtOTN4WkJSaTA5M203VnNsSVBaS2JtSTJ4LWtYSVNOeGV0R0tXZVIxWGtaTEljejB0aGRrU2tPNDBQYjVJUzVBN3hTOGUxNEpvQ21JNk11M0ZueG9rTm55QXdDZHFWOHk3Yi1oVjZFUTI5UDdWQnlURGo5bzYwZncifSwibm9uY2UiOiJNVFU0T0RBek1qZ3dOekF4Tmc9PSIsInVybCI6Imh0dHA6Ly8xOTIuMTY4LjEuMTM6ODE4MS9hY21lL25ldy1hY2NvdW50In0\",\n" +
            "   \"payload\":\"eyJ0ZXJtc09mU2VydmljZUFncmVlZCI6dHJ1ZSwiY29udGFjdCI6WyJtYWlsdG86dGVzdEB0ZXN0LmNvbSJdLCJleHRlcm5hbEFjY291bnRCaW5kaW5nIjp7InByb3RlY3RlZCI6ImV5SmhiR2NpT2lKSVV6STFOaUlzSW10cFpDSTZJbk4zYzFSUWFXMWlTRXRuTldWQ2VHeE5RMkZNSWl3aWRYSnNJam9pYUhSMGNEb3ZMekU1TWk0eE5qZ3VNUzR4TXpvNE1UZ3hMMkZqYldVdmJtVjNMV0ZqWTI5MWJuUWlmUSIsInBheWxvYWQiOiJleUpsSWpvaVFWRkJRaUlzSW10MGVTSTZJbEpUUVNJc0ltNGlPaUozWDFKWWVsZGxWVlJPUTBOeFVrWlNYMnR0T1V4SWNIaHRXVTFuUjB4RGFqYzRSek5RY0VndE1VZEhRVXRTVUZWcGFGVk1ja2RSZGpWMGFUYzBRV1pQYjJaVGJHUkhUamxCVEZndFUwdHljbEZZVFVOb01qSTNaVWw0UmpoR1MxSlJSMlJGVldwcU9IVnBkVUZXU1RaM2RuSlhUV2hNY1V0elgzaDFTSGc0Y1hONVNUZzVNMnAxUXpoTVUyUmxkVzlmYjBadWVIRk1SMEl5V1daS05tZzNTWFppTmxoQmJHd3ROMDlZUmpkSVYwUTVlRFp2ZEVGb09VczBVSFF4VmxwQmVFUnVRbmhXVDJGaE5uTmxaRUY0Um0xUU1HRTVZMGRFTUZGS1luZ3RPVE40V2tKU2FUQTVNMjAzVm5Oc1NWQmFTMkp0U1RKNExXdFlTVk5PZUdWMFIwdFhaVkl4V0d0YVRFbGplakIwYUdSclUydFBOREJRWWpWSlV6VkJOM2hUT0dVeE5FcHZRMjFKTmsxMU0wWnVlRzlyVG01NVFYZERaSEZXT0hrM1lpMW9WalpGVVRJNVVEZFdRbmxVUkdvNWJ6WXdabmNpZlEiLCJzaWduYXR1cmUiOiJyTklDVjMxLXY2Smotd1c1SXNwc1N4MUl2VjllUmp6cjdaai1uVWlidl9VIn19\",\n" +
            "   \"signature\":\"pfrVCdwOL14r9BvrDmu8PjpWhOAQckeskXvwF__DecrGtHGnMiut6prkTG7rSmPC7sm9RnFv10G-WsEVyRjRTiC1BPdIbr9zOEDRibHBXsmdc4vfJKyenpQOB3pDeNqhy9srxbae0Q2fAVBxX-t12sfsVS1Y6hTmVZqwrRBfk7zhoJftOHEllC6I5YQPqGYcIDzHbddAEdrxuJVLjNnf1G3IBilELxfgXcZi1kB9Qt0BVCHs1RrE_PDNFUrABtToBUjP7g5J2GGJ44zZ1xfvkwFXOhxFyQV9Yi2qYo0EjN9m4tz6jMU-95kb8mikbdfX7bRbNx9yS79HaoCHNHdZkQ\"\n" +
            "}";

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private SecurityValidatorUtil securityValidatorUtil;
    @Autowired
    private DirectoryDataSettingsPersistence directoryDataSettingsPersistence;
    @Autowired
    private DirectoryDataService directoryDataService;
    @Autowired
    private AccountPersistence accountPersistence;
    @MockBean
    private ExternalAccountProviderService externalAccountProviderService;

    @BeforeEach
    public void before() throws Exception {
        when(externalAccountProviderService.findByName("test")).thenReturn(new MockExternalAccountProvider());

        DirectoryDataSettings directoryDataSettings = new DirectoryDataSettings();
        directoryDataSettings.setName("acme-test");
        directoryDataSettings.setMetaExternalAccountRequired(true);
        directoryDataSettings.setExternalAccountProviderName("test");
        directoryDataSettings = directoryDataSettingsPersistence.save(directoryDataSettings);
        directoryDataService.load(directoryDataSettings);

        DirectoryDataSettings directoryDataSettings2 = new DirectoryDataSettings();
        directoryDataSettings2.setName("acme-test-no-eab");
        directoryDataSettings.setMetaExternalAccountRequired(false);
        directoryDataSettings2 = directoryDataSettingsPersistence.save(directoryDataSettings2);
        directoryDataService.load(directoryDataSettings2);
    }

    @AfterEach
    public void after(){
        directoryDataService.delete("acme-test");
        directoryDataService.delete("acme-test-no-eab");
    }

    @Test
    public void request() throws Exception {
        AccountData accountData = MockUtils.buildMockAccountData();
        ExternalAccountBinding eab = MockUtils.buildMockExternalAccountBinding("http://localhost/acme-test/new-account");

        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setContact(new String[]{"mailto:user@winllc-dev.com"});
        accountRequest.setTermsOfServiceAgreed(true);
        accountData.getObject().setContact(accountRequest.getContact());
        accountRequest.setExternalAccountBinding(eab);

        JWSObject jwsObject = MockUtils.buildCustomAcmeJwsObject(accountRequest,
                "http://localhost/acme-test/new-account");
        String json = MockUtils.jwsObjectAsString(jwsObject);

        MvcResult result = mockMvc.perform(
                post("/acme-test/new-account")
                        .contentType("application/jose+json")
                        .content(json))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        assertEquals(201, result.getResponse().getStatus());
        assertTrue(responseString.contains("\"status\":\"valid\""));
        assertTrue(responseString.contains(accountRequest.getContact()[0]));
        assertTrue(responseString.contains("\"orders\""));
    }

    @Test
    public void requestNoExternalAccountBinding() throws Exception {
        //todo set custom directory data with no external account binding
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setContact(new String[]{"mailto:user@winllc-dev.com"});
        accountRequest.setTermsOfServiceAgreed(true);

        JWSObject jwsObject = MockUtils.buildCustomAcmeJwsObject(accountRequest,
                "http://localhost/acme-test-no-eab/new-account");
        String json = MockUtils.jwsObjectAsString(jwsObject);

        MvcResult result = mockMvc.perform(
                post("/acme-test-no-eab/new-account")
                        .contentType("application/jose+json")
                        .content(json))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        assertTrue(responseString.contains("\"status\":\"valid\""));
        assertTrue(responseString.contains(accountRequest.getContact()[0]));
        assertTrue(responseString.contains("\"orders\""));
    }

    //@Test
    public void updateTermsOfServiceChanged() throws Exception {
        DirectoryDataSettings directoryDataSettings = directoryDataSettingsPersistence.findByName("acme-test-no-eab");
        directoryDataSettings.setMetaTermsOfService("http://localhost/acme-test-no-eab/tos");
        directoryDataService.load(directoryDataSettings);

        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setContact(new String[]{"mailto:user@winllc-dev.com"});
        accountRequest.setTermsOfServiceAgreed(true);

        JWSObject jwsObject = MockUtils.buildCustomAcmeJwsObject(accountRequest,
                "http://localhost/acme-test-no-eab/new-account");
        String json = MockUtils.jwsObjectAsString(jwsObject);

        MvcResult result = mockMvc.perform(
                post("/acme-test/acct/1")
                        .contentType("application/jose+json")
                        .content(json))
                .andReturn();
        assertEquals(403, result.getResponse().getStatus());
    }

    @Test
    public void update() throws Exception {
        AccountData accountData = MockUtils.buildMockAccountData();
        DirectoryData directoryData = MockUtils.buildMockDirectoryData(false);

        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setContact(new String[]{"mailto:test@test.com"});
        accountData.getObject().setContact(accountRequest.getContact());

        PayloadAndAccount<AccountRequest> payloadAndAccount = new PayloadAndAccount<>(accountRequest, accountData, directoryData);
        when(securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(any(HttpServletRequest.class), any(String.class),
                isA(Class.class))).thenReturn(payloadAndAccount);


        JWSObject jwsObject = MockUtils.buildCustomJwsObject(accountRequest, "http://localhost/acme-test/acct/"+accountData.getId());
        String json = MockUtils.jwsObjectAsString(jwsObject);

        MvcResult result = mockMvc.perform(
                post("/acme-test/acct/1")
                        .contentType("application/jose+json")
                        .content(json))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        assertEquals(200, result.getResponse().getStatus());
        assertTrue(responseString.contains(accountRequest.getContact()[0]));
    }

    @Test
    public void keyRollover() throws Exception {
        AccountData accountData = MockUtils.buildMockAccountData();
        DirectoryData directoryData = MockUtils.buildMockDirectoryData(false);

        KeyChangeRequest keyChangeRequest = new KeyChangeRequest();
        keyChangeRequest.setAccount(accountData.buildUrl(Application.baseURL));
        keyChangeRequest.setOldKey(accountData.getJwk());

        AcmeJWSObject jwsObject = MockUtils.buildCustomAcmeJwsObjectWithAlternateJwk(keyChangeRequest,
                "/acme-test/key-change", accountData.buildUrl(Application.baseURL));

        String json = MockUtils.jwsObjectAsString(jwsObject);

        PayloadAndAccount<AcmeJWSObject> payloadAndAccount = new PayloadAndAccount<>(jwsObject, accountData, directoryData);

        when(securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(any(HttpServletRequest.class),
                any(Class.class))).thenReturn(payloadAndAccount);

        mockMvc.perform(
                post("/acme-test/key-change")
                        .contentType("application/jose+json")
                        .content(json))
                .andExpect(status().is(200));

        //Set back after change for other tests
        accountData.setJwk(MockUtils.rsaJWK.toPublicJWK().toString());
    }
}