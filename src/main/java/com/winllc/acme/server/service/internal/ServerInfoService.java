package com.winllc.acme.server.service.internal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/serverInfo")
public class ServerInfoService {

    @Value("{app.baseUrl}")
    private String baseUrl;

    @GetMapping("/baseUrl")
    public String getBaseUrl(){
        return baseUrl;
    }
}
