package com.winllc.acme.server.external;

import com.winllc.acme.common.CAValidationRule;
import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.server.model.acme.Identifier;
import org.apache.commons.lang3.StringUtils;

public abstract class AbstractCertAuthority implements CertificateAuthority {

    protected String name;
    protected CertificateAuthoritySettings settings;


    protected AbstractCertAuthority(CertificateAuthoritySettings settings){
        this.settings = settings;
    }

    @Override
    public String getName() {
        return settings.getName();
    }

    protected boolean canIssueToIdentifier(Identifier identifier, CAValidationRule validationRule){

        if(!identifier.getValue().contains(".") && validationRule.isAllowHostnameIssuance()){
            return true;
        }

        if(StringUtils.isNotBlank(validationRule.getIdentifierType()) && StringUtils.isNotBlank(validationRule.getBaseDomainName()) &&
                identifier.getType().contentEquals(validationRule.getIdentifierType()) && identifier.getValue().endsWith(validationRule.getBaseDomainName())){
            return validationRule.isAllowIssuance();
        }else{
            return false;
        }
    }
}
