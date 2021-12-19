package com.winllc.acme.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Application {

    public static String baseURL;

    @Value("${app.baseUrl}")
    public void setTempBaseUrl(String tempBaseUrl) {
        Application.baseURL = tempBaseUrl;
    }
}
