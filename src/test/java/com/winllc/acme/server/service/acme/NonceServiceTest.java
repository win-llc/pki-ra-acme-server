package com.winllc.acme.server.service.acme;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NonceService.class)
public class NonceServiceTest extends AbstractServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void newNonceHead() {
        //todo
    }

    @Test
    public void newNonceGet() throws Exception {
        mockMvc.perform(
                get("/acme-test/new-nonce"))
                //.contentType("application/jose+json"))
                //.content(json))
                .andExpect(status().is(204));
    }
}