package com.winllc.acme.server.util;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NonceUtilTest {

    @Test
    public void testNonce(){
        String nonce = NonceUtil.generateNonce();

        assertFalse(NonceUtil.checkNonceUsed(nonce));

        NonceUtil.markNonceUsed(nonce);

        assertTrue(NonceUtil.checkNonceUsed(nonce));
    }
}