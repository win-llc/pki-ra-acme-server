package com.winllc.acme.server.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.Base64URL;
import com.winllc.acme.common.contants.StatusType;
import com.winllc.acme.common.model.AcmeJWSObject;
import com.winllc.acme.common.model.acme.Account;
import com.winllc.acme.common.model.acme.Directory;
import com.winllc.acme.common.model.acme.Meta;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.common.model.requestresponse.AccountRequest;
import com.winllc.acme.common.util.SecurityUtil;
import com.winllc.acme.server.configuration.AppConfig;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.persistence.AccountPersistence;
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

import java.text.ParseException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class SecurityValidatorUtilTest {

    private static String newAccountRequest = "{\n" +
            "   \"protected\":\"eyJhbGciOiJSUzI1NiIsImp3ayI6eyJlIjoiQVFBQiIsImt0eSI6IlJTQSIsIm4iOiJ3X1JYeldlVVROQ0NxUkZSX2ttOUxIcHhtWU1nR0xDajc4RzNQcEgtMUdHQUtSUFVpaFVMckdRdjV0aTc0QWZPb2ZTbGRHTjlBTFgtU0tyclFYTUNoMjI3ZUl4RjhGS1JRR2RFVWpqOHVpdUFWSTZ3dnJXTWhMcUtzX3h1SHg4cXN5STg5M2p1QzhMU2RldW9fb0ZueHFMR0IyWWZKNmg3SXZiNlhBbGwtN09YRjdIV0Q5eDZvdEFoOUs0UHQxVlpBeERuQnhWT2FhNnNlZEF4Rm1QMGE5Y0dEMFFKYngtOTN4WkJSaTA5M203VnNsSVBaS2JtSTJ4LWtYSVNOeGV0R0tXZVIxWGtaTEljejB0aGRrU2tPNDBQYjVJUzVBN3hTOGUxNEpvQ21JNk11M0ZueG9rTm55QXdDZHFWOHk3Yi1oVjZFUTI5UDdWQnlURGo5bzYwZncifSwibm9uY2UiOiJNVFU0T0RBek1qZ3dOekF4Tmc9PSIsInVybCI6Imh0dHA6Ly8xOTIuMTY4LjEuMTM6ODE4MS9hY21lL25ldy1hY2NvdW50In0\",\n" +
            "   \"payload\":\"eyJ0ZXJtc09mU2VydmljZUFncmVlZCI6dHJ1ZSwiY29udGFjdCI6WyJtYWlsdG86dGVzdEB0ZXN0LmNvbSJdLCJleHRlcm5hbEFjY291bnRCaW5kaW5nIjp7InByb3RlY3RlZCI6ImV5SmhiR2NpT2lKSVV6STFOaUlzSW10cFpDSTZJbk4zYzFSUWFXMWlTRXRuTldWQ2VHeE5RMkZNSWl3aWRYSnNJam9pYUhSMGNEb3ZMekU1TWk0eE5qZ3VNUzR4TXpvNE1UZ3hMMkZqYldVdmJtVjNMV0ZqWTI5MWJuUWlmUSIsInBheWxvYWQiOiJleUpsSWpvaVFWRkJRaUlzSW10MGVTSTZJbEpUUVNJc0ltNGlPaUozWDFKWWVsZGxWVlJPUTBOeFVrWlNYMnR0T1V4SWNIaHRXVTFuUjB4RGFqYzRSek5RY0VndE1VZEhRVXRTVUZWcGFGVk1ja2RSZGpWMGFUYzBRV1pQYjJaVGJHUkhUamxCVEZndFUwdHljbEZZVFVOb01qSTNaVWw0UmpoR1MxSlJSMlJGVldwcU9IVnBkVUZXU1RaM2RuSlhUV2hNY1V0elgzaDFTSGc0Y1hONVNUZzVNMnAxUXpoTVUyUmxkVzlmYjBadWVIRk1SMEl5V1daS05tZzNTWFppTmxoQmJHd3ROMDlZUmpkSVYwUTVlRFp2ZEVGb09VczBVSFF4VmxwQmVFUnVRbmhXVDJGaE5uTmxaRUY0Um0xUU1HRTVZMGRFTUZGS1luZ3RPVE40V2tKU2FUQTVNMjAzVm5Oc1NWQmFTMkp0U1RKNExXdFlTVk5PZUdWMFIwdFhaVkl4V0d0YVRFbGplakIwYUdSclUydFBOREJRWWpWSlV6VkJOM2hUT0dVeE5FcHZRMjFKTmsxMU0wWnVlRzlyVG01NVFYZERaSEZXT0hrM1lpMW9WalpGVVRJNVVEZFdRbmxVUkdvNWJ6WXdabmNpZlEiLCJzaWduYXR1cmUiOiJyTklDVjMxLXY2Smotd1c1SXNwc1N4MUl2VjllUmp6cjdaai1uVWlidl9VIn19\",\n" +
            "   \"signature\":\"pfrVCdwOL14r9BvrDmu8PjpWhOAQckeskXvwF__DecrGtHGnMiut6prkTG7rSmPC7sm9RnFv10G-WsEVyRjRTiC1BPdIbr9zOEDRibHBXsmdc4vfJKyenpQOB3pDeNqhy9srxbae0Q2fAVBxX-t12sfsVS1Y6hTmVZqwrRBfk7zhoJftOHEllC6I5YQPqGYcIDzHbddAEdrxuJVLjNnf1G3IBilELxfgXcZi1kB9Qt0BVCHs1RrE_PDNFUrABtToBUjP7g5J2GGJ44zZ1xfvkwFXOhxFyQV9Yi2qYo0EjN9m4tz6jMU-95kb8mikbdfX7bRbNx9yS79HaoCHNHdZkQ\"\n" +
            "}";

    @Autowired
    private SecurityValidatorUtil util;

    @MockBean
    private AccountPersistence accountPersistence;
    @MockBean
    private DirectoryDataService directoryDataService;

    @BeforeEach
    public void before(){
        Directory directory = new Directory();
        directory.setNewNonce("https://example.com/acme/new-nonce");
        directory.setNewAccount("https://example.com/acme/new-account");
        directory.setNewOrder("https://example.com/acme/new-order");
        directory.setNewAuthz("https://example.com/acme/new-authz");
        directory.setRevokeCert("https://example.com/acme/revoke-cert");
        directory.setKeyChange("https://example.com/acme/key-change");

        Meta meta = new Meta();
        meta.setExternalAccountRequired(false);
        directory.setMeta(meta);

        DirectoryData directoryData = new DirectoryData(directory);
        directoryData.setName("acme-test");
        directoryData.setAllowPreAuthorization(false);

        Account account = new Account();
        account.setStatus(StatusType.VALID.toString());
        AccountData accountData = new AccountData(account, "acme");
        accountData.setJwk("{\"kty\":\"RSA\",\"e\":\"AQAB\",\"n\":\"w_RXzWeUTNCCqRFR_km9LHpxmYMgGLCj78G3PpH-1GGAKRPUihULrGQv5ti74AfOofSldGN9ALX-SKrrQXMCh227eIxF8FKRQGdEUjj8uiuAVI6wvrWMhLqKs_xuHx8qsyI893juC8LSdeuo_oFnxqLGB2YfJ6h7Ivb6XAll-7OXF7HWD9x6otAh9K4Pt1VZAxDnBxVOaa6sedAxFmP0a9cGD0QJbx-93xZBRi093m7VslIPZKbmI2x-kXISNxetGKWeR1XkZLIcz0thdkSkO40Pb5IS5A7xS8e14JoCmI6Mu3FnxokNnyAwCdqV8y7b-hV6EQ29P7VByTDj9o60fw\"}");

        when(directoryDataService.getByName(any())).thenReturn(Optional.of(directoryData));
        when(accountPersistence.findById("account1")).thenReturn(Optional.of(accountData));
        when(accountPersistence.findFirstByJwkEquals(any())).thenReturn(Optional.of(accountData));
    }


    @Test
    public void generateRandomString() {
        String random = SecurityUtil.generateRandomString(10);
        assertEquals(random.length(), 10);
    }

    @Test
    public void verifyJWSAndReturnPayloadForExistingAccount() throws JsonProcessingException, ParseException, AcmeServerException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = objectMapper.readTree(newAccountRequest);
        String protectedUrl = node.get("protected").asText();
        String payloadUrl = node.get("payload").asText();
        String signatureUrl = node.get("signature").asText();

        AcmeJWSObject jwsObject = new AcmeJWSObject(new Base64URL(protectedUrl), new Base64URL(payloadUrl), new Base64URL(signatureUrl));
        String requestUrl = "http://192.168.1.13:8181/acme/new-account";
        String accountId = "account1";
        Class<AccountRequest> clazz = AccountRequest.class;

        PayloadAndAccount<AccountRequest> payloadAndAccount = util.verifyJWSAndReturnPayloadForExistingAccount(jwsObject, requestUrl, accountId, clazz);
        assertTrue(payloadAndAccount != null);
    }

}