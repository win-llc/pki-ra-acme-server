package com.winllc.acme.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class Application {

    public static String baseURL = "http://192.168.1.13:8181/";

    @Value("${app.baseUrl}")
    public static void setBaseURL(String baseURL) {
        Application.baseURL = baseURL;
    }
}
