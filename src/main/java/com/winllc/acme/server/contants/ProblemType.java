package com.winllc.acme.server.contants;

public enum ProblemType {
    ACCOUNT_DOES_NOT_EXIST("accountDoesNotExist"),
    ALREADY_REVOKED("alreadyRevoked"),
    BAD_CSR("badCSR"),
    BAD_NONCE("badNonce"),
    BAD_PUBLIC_KEY("badPublicKey"),
    BAD_REVOCATION_REASON("badRevocationReason"),
    BAD_SIGNATURE_ALGORITHM("badSignatureAlgorithm"),
    CAA("caa"),
    COMPOUND("compound"),
    CONNECTION("connection"),
    DNS("dns"),
    EXTERNAL_ACCOUNT_REQUIRED("externalAccountRequired"),
    INCORRECT_RESPONSE("incorrectResponse"),
    INVALID_CONTACT("invalidContact"),
    MALFORMED("malformed"),
    ORDER_NOT_READY("orderNotReady"),
    RATE_LIMITED("rateLimited"),
    REJECTED_IDENTIFIER("rejectedIdentifier"),
    SERVER_INTERNAL("serverInternal"),
    TLS("tls"),
    UNAUTHORIZED("unauthorized"),
    UNSUPPORTED_IDENTIFIER("unsopportedIdentifier"),
    USER_ACTION_REQUIRED("userActionRequired");

    private static final String baseError = "urn:ietf:params:acme:error:";
    private String value;

    ProblemType(String value) {
        this.value = baseError+value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
