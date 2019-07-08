package com.winllc.acme.server.contants;

public enum RevocationReason {
    UNSPECIFIED("unspecified", 0),
    KEY_COMPROMISE("keyCompromise", 1),
    CA_COMPROMISE("cACompromise", 2),
    AFFILIATION_CHANGED("affiliationChanged", 3),
    SUPERSEDED("superseded", 4),
    CESSATION_OF_OPERATION("cessationOfOperation", 5),
    CERTIFICATE_HOLD("certificateHold", 6),
    REMOVE_FROM_CRL("removeFromCRL", 8),
    PRIVILEGE_WITHDRAWN("privilegeWithdrawn", 9),
    AA_COMPROMISE("aACompromise", 10);

    private String value;
    private int code;

    RevocationReason(String value, int code) {
        this.value = value;
        this.code = code;
    }

    public String getValue() {
        return value;
    }

    public int getCode() {
        return code;
    }

    public static RevocationReason fromCode(int code){
        for(RevocationReason reason : values()){
            if(reason.code == code) return reason;
        }
        return null;
    }
}
