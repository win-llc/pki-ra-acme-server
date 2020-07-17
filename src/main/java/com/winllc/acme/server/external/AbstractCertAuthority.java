package com.winllc.acme.server.external;

import com.winllc.acme.common.AcmeCertAuthorityType;
import com.winllc.acme.common.CertIssuanceValidationRule;
import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.common.contants.ChallengeType;
import com.winllc.acme.common.contants.ProblemType;
import com.winllc.acme.common.model.acme.Directory;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.common.model.acme.Identifier;
import com.winllc.acme.common.model.data.AccountData;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractCertAuthority implements CertificateAuthority {

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

    protected boolean canIssueToIdentifier(Identifier identifier, CertIssuanceValidationRule validationRule){

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
    public List<ChallengeType> getIdentifierChallengeRequirements(Identifier identifier, AccountData accountData, DirectoryData directoryData)
            throws AcmeServerException {
        Set<ChallengeType> challengeTypes = new HashSet<>();
        if(canIssueToIdentifier(identifier, accountData, directoryData)){
            for (CertIssuanceValidationRule rule : getValidationRules(accountData, directoryData).getCertIssuanceValidationRules()) {
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
