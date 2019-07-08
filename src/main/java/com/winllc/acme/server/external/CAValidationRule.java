package com.winllc.acme.server.external;

//A set of rules will be associated with each CA, rules dictate how challenges created for orders
public class CAValidationRule {
    private String identifierType;
    private String baseDomainName;
    private boolean requireDnsChallenge;
    private boolean requireHttpChallenge;

    public String getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(String identifierType) {
        this.identifierType = identifierType;
    }

    public String getBaseDomainName() {
        return baseDomainName;
    }

    public void setBaseDomainName(String baseDomainName) {
        this.baseDomainName = baseDomainName;
    }

    public boolean isRequireDnsChallenge() {
        return requireDnsChallenge;
    }

    public void setRequireDnsChallenge(boolean requireDnsChallenge) {
        this.requireDnsChallenge = requireDnsChallenge;
    }

    public boolean isRequireHttpChallenge() {
        return requireHttpChallenge;
    }

    public void setRequireHttpChallenge(boolean requireHttpChallenge) {
        this.requireHttpChallenge = requireHttpChallenge;
    }
}
