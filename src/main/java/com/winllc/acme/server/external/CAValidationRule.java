package com.winllc.acme.server.external;

import com.winllc.acme.server.model.acme.Identifier;
import org.apache.commons.lang3.StringUtils;

//A set of rules will be associated with each CA, rules dictate how challenges created for orders
public class CAValidationRule {
    private String identifierType;
    private String baseDomainName;
    private boolean requireDnsChallenge;
    private boolean requireHttpChallenge;
    private boolean allowIssuance;
    private boolean allowHostnameIssuance;

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

    public boolean isAllowIssuance() {
        return allowIssuance;
    }

    public void setAllowIssuance(boolean allowIssuance) {
        this.allowIssuance = allowIssuance;
    }

    public boolean isAllowHostnameIssuance() {
        return allowHostnameIssuance;
    }

    public void setAllowHostnameIssuance(boolean allowHostnameIssuance) {
        this.allowHostnameIssuance = allowHostnameIssuance;
    }

    public boolean canIssueToIdentifier(Identifier identifier){
        if(!identifier.getValue().contains(".") && allowHostnameIssuance){
            return true;
        }

        if(StringUtils.isNotBlank(identifierType) && StringUtils.isNotBlank(baseDomainName) &&
                identifier.getType().contentEquals(identifierType) && identifier.getValue().endsWith(baseDomainName)){
            return allowIssuance;
        }else{
            return false;
        }
    }
}
