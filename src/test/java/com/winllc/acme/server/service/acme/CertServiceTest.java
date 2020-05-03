package com.winllc.acme.server.service.acme;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.Assert.*;

@WebMvcTest(CertService.class)
public class CertServiceTest extends AbstractServiceTest {

    private static String certRequest = "{\n" +
            "   \"protected\":\"eyJhbGciOiJSUzI1NiIsImp3ayI6eyJlIjoiQVFBQiIsImt0eSI6IlJTQSIsIm4iOiJ3X1JYeldlVVROQ0NxUkZSX2ttOUxIcHhtWU1nR0xDajc4RzNQcEgtMUdHQUtSUFVpaFVMckdRdjV0aTc0QWZPb2ZTbGRHTjlBTFgtU0tyclFYTUNoMjI3ZUl4RjhGS1JRR2RFVWpqOHVpdUFWSTZ3dnJXTWhMcUtzX3h1SHg4cXN5STg5M2p1QzhMU2RldW9fb0ZueHFMR0IyWWZKNmg3SXZiNlhBbGwtN09YRjdIV0Q5eDZvdEFoOUs0UHQxVlpBeERuQnhWT2FhNnNlZEF4Rm1QMGE5Y0dEMFFKYngtOTN4WkJSaTA5M203VnNsSVBaS2JtSTJ4LWtYSVNOeGV0R0tXZVIxWGtaTEljejB0aGRrU2tPNDBQYjVJUzVBN3hTOGUxNEpvQ21JNk11M0ZueG9rTm55QXdDZHFWOHk3Yi1oVjZFUTI5UDdWQnlURGo5bzYwZncifSwibm9uY2UiOiJNVFU0T0RBek1qazVOakF5T1E9PSIsInVybCI6Imh0dHA6Ly8xOTIuMTY4LjEuMTM6ODE4MS9hY21lL2NlcnQvMEpWcW5MUmg1RiJ9\",\n" +
            "   \"payload\":\"\",\n" +
            "   \"signature\":\"PbU4IGvz49mUvVAoCwI_X2RQcVj28vtptSAcOweQnet-cg-fZyYN3ob8IwxNHWVGtCpkMp4hKD1Mq23PSh_U1-IjB0M-U0kDZRawSdy2VFIFRTNzt2eUMlefeKpkChk-W3W5kANJKuL2_iAd5Slt4Cq-2EOHsnbRfTNJe_glkDzYiOBGw9y_IhCb4rqZT_7s4unpNA8jzOyu1NjcROnQfZfmXmXJW7qwy4EJBxZ4ibQKuU4bdGtBRvADpymeZw0Y1VhfuR4qXT5bB32Tsmok76fJ3fruNB0eM6MLPWm1xbmwCgaSch2KgiHfkT76LFdzGJ5fTjcUUlNVBXtddfpWbw\"\n" +
            "}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void certDownload() {
        //todo
    }

    @Test
    public void certRevoke() {
        //todo
    }
}