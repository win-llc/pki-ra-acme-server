package com.winllc.acme.server.external;

import com.winllc.acme.common.AcmeCertAuthorityType;
import com.winllc.acme.common.CAValidationRule;
import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.server.contants.ChallengeType;
import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.model.acme.Identifier;
import com.winllc.acme.server.model.data.AccountData;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractCertAuthority implements CertificateAuthority {

    protected String name;
    protected AcmeCertAuthorityType type;
    protected CertificateAuthoritySettings settings;


    protected AbstractCertAuthority(CertificateAuthoritySettings settings){
        this.settings = settings;
    }

    @Override
    public String getName() {
        return settings.getName();
    }

    @Override
    public AcmeCertAuthorityType getType() {
        return AcmeCertAuthorityType.valueOf(settings.getType());
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

    @Override
    public List<ChallengeType> getIdentifierChallengeRequirements(Identifier identifier, AccountData accountData) throws AcmeServerException {
        Set<ChallengeType> challengeTypes = new HashSet<>();
        if(canIssueToIdentifier(identifier, accountData)){
            for (CAValidationRule rule : getValidationRules(accountData).getCaValidationRules()) {
                if(canIssueToIdentifier(identifier, rule)){
                    if(rule.isRequireHttpChallenge()) challengeTypes.add(ChallengeType.HTTP);
                    if(rule.isRequireDnsChallenge()) challengeTypes.add(ChallengeType.DNS);
                }
            }
            return new ArrayList<>(challengeTypes);
        }else{
            throw new AcmeServerException(ProblemType.REJECTED_IDENTIFIER, identifier.getValue());
        }
    }
}
