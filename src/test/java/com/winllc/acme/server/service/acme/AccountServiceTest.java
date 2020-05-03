package com.winllc.acme.server.service.acme;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountService.class)
public class AccountServiceTest extends AbstractServiceTest {

    private static String newAccountRequest = "{\n" +
            "   \"protected\":\"eyJhbGciOiJSUzI1NiIsImp3ayI6eyJlIjoiQVFBQiIsImt0eSI6IlJTQSIsIm4iOiJ3X1JYeldlVVROQ0NxUkZSX2ttOUxIcHhtWU1nR0xDajc4RzNQcEgtMUdHQUtSUFVpaFVMckdRdjV0aTc0QWZPb2ZTbGRHTjlBTFgtU0tyclFYTUNoMjI3ZUl4RjhGS1JRR2RFVWpqOHVpdUFWSTZ3dnJXTWhMcUtzX3h1SHg4cXN5STg5M2p1QzhMU2RldW9fb0ZueHFMR0IyWWZKNmg3SXZiNlhBbGwtN09YRjdIV0Q5eDZvdEFoOUs0UHQxVlpBeERuQnhWT2FhNnNlZEF4Rm1QMGE5Y0dEMFFKYngtOTN4WkJSaTA5M203VnNsSVBaS2JtSTJ4LWtYSVNOeGV0R0tXZVIxWGtaTEljejB0aGRrU2tPNDBQYjVJUzVBN3hTOGUxNEpvQ21JNk11M0ZueG9rTm55QXdDZHFWOHk3Yi1oVjZFUTI5UDdWQnlURGo5bzYwZncifSwibm9uY2UiOiJNVFU0T0RBek1qZ3dOekF4Tmc9PSIsInVybCI6Imh0dHA6Ly8xOTIuMTY4LjEuMTM6ODE4MS9hY21lL25ldy1hY2NvdW50In0\",\n" +
            "   \"payload\":\"eyJ0ZXJtc09mU2VydmljZUFncmVlZCI6dHJ1ZSwiY29udGFjdCI6WyJtYWlsdG86dGVzdEB0ZXN0LmNvbSJdLCJleHRlcm5hbEFjY291bnRCaW5kaW5nIjp7InByb3RlY3RlZCI6ImV5SmhiR2NpT2lKSVV6STFOaUlzSW10cFpDSTZJbk4zYzFSUWFXMWlTRXRuTldWQ2VHeE5RMkZNSWl3aWRYSnNJam9pYUhSMGNEb3ZMekU1TWk0eE5qZ3VNUzR4TXpvNE1UZ3hMMkZqYldVdmJtVjNMV0ZqWTI5MWJuUWlmUSIsInBheWxvYWQiOiJleUpsSWpvaVFWRkJRaUlzSW10MGVTSTZJbEpUUVNJc0ltNGlPaUozWDFKWWVsZGxWVlJPUTBOeFVrWlNYMnR0T1V4SWNIaHRXVTFuUjB4RGFqYzRSek5RY0VndE1VZEhRVXRTVUZWcGFGVk1ja2RSZGpWMGFUYzBRV1pQYjJaVGJHUkhUamxCVEZndFUwdHljbEZZVFVOb01qSTNaVWw0UmpoR1MxSlJSMlJGVldwcU9IVnBkVUZXU1RaM2RuSlhUV2hNY1V0elgzaDFTSGc0Y1hONVNUZzVNMnAxUXpoTVUyUmxkVzlmYjBadWVIRk1SMEl5V1daS05tZzNTWFppTmxoQmJHd3ROMDlZUmpkSVYwUTVlRFp2ZEVGb09VczBVSFF4VmxwQmVFUnVRbmhXVDJGaE5uTmxaRUY0Um0xUU1HRTVZMGRFTUZGS1luZ3RPVE40V2tKU2FUQTVNMjAzVm5Oc1NWQmFTMkp0U1RKNExXdFlTVk5PZUdWMFIwdFhaVkl4V0d0YVRFbGplakIwYUdSclUydFBOREJRWWpWSlV6VkJOM2hUT0dVeE5FcHZRMjFKTmsxMU0wWnVlRzlyVG01NVFYZERaSEZXT0hrM1lpMW9WalpGVVRJNVVEZFdRbmxVUkdvNWJ6WXdabmNpZlEiLCJzaWduYXR1cmUiOiJyTklDVjMxLXY2Smotd1c1SXNwc1N4MUl2VjllUmp6cjdaai1uVWlidl9VIn19\",\n" +
            "   \"signature\":\"pfrVCdwOL14r9BvrDmu8PjpWhOAQckeskXvwF__DecrGtHGnMiut6prkTG7rSmPC7sm9RnFv10G-WsEVyRjRTiC1BPdIbr9zOEDRibHBXsmdc4vfJKyenpQOB3pDeNqhy9srxbae0Q2fAVBxX-t12sfsVS1Y6hTmVZqwrRBfk7zhoJftOHEllC6I5YQPqGYcIDzHbddAEdrxuJVLjNnf1G3IBilELxfgXcZi1kB9Qt0BVCHs1RrE_PDNFUrABtToBUjP7g5J2GGJ44zZ1xfvkwFXOhxFyQV9Yi2qYo0EjN9m4tz6jMU-95kb8mikbdfX7bRbNx9yS79HaoCHNHdZkQ\"\n" +
            "}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void request() throws Exception {

        mockMvc.perform(
                post("/acme-test/new-account")
                        .contentType("application/jose+json")
                        .content(newAccountRequest))
                .andExpect(status().is(201));
    }

    @Test
    public void update() {
        //todo
    }

    @Test
    public void keyRollover() {
        //todo
    }
}