package com.winllc.acme.server.service.acme;

import com.winllc.acme.server.service.AbstractServiceTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DirectoryService.class)
public class DirectoryServiceTest extends AbstractServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void directory() throws Exception {

        mockMvc.perform(
                get("/acme-test/directory"))
                        //.contentType("application/jose+json"))
                //.content(json))
                .andExpect(status().is(200));

    }
}