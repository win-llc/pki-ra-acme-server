package com.winllc.acme.server.util;


import com.winllc.acme.server.configuration.AppConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class NonceUtilTest {

    @Autowired
    private NonceUtil nonceUtil;

    @Test
    public void testNonce(){
        String nonce = nonceUtil.generateNonce();

        assertFalse(nonceUtil.checkNonceUsed(nonce));

        nonceUtil.markNonceUsed(nonce);

        assertTrue(nonceUtil.checkNonceUsed(nonce));
    }
}