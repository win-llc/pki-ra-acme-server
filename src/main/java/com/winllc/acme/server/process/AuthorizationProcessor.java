package com.winllc.acme.server.process;

import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.external.CAValidationRule;
import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.acme.server.model.acme.Authorization;
import com.winllc.acme.server.model.acme.Challenge;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.acme.Identifier;
import com.winllc.acme.server.model.data.AuthorizationData;
import com.winllc.acme.server.model.data.ChallengeData;
import com.winllc.acme.server.model.data.DataObject;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.persistence.AuthorizationPersistence;
import com.winllc.acme.server.persistence.ChallengePersistence;
import com.winllc.acme.server.service.internal.CertificateAuthorityService;

import java.util.List;
import java.util.Optional;

/*
                   pending --------------------+
                      |                        |
    Challenge failure |                        |
           or         |                        |
          Error       |  Challenge valid       |
            +---------+---------+              |
            |                   |              |
            V                   V              |
         invalid              valid            |
                                |              |
                                |              |
                                |              |
                 +--------------+--------------+
                 |              |              |
                 |              |              |
          Server |       Client |   Time after |
          revoke |   deactivate |    "expires" |
                 V              V              V
              revoked      deactivated      expired
 */

public class AuthorizationProcessor implements AcmeDataProcessor<AuthorizationData> {

    private ChallengeProcessor challengeProcessor;

    @Override
    public AuthorizationData buildNew(DirectoryData directoryData) {
        Authorization authorization = new Authorization();
        authorization.setStatus("pending");

        AuthorizationData authorizationData = new AuthorizationData(authorization, directoryData);

        return authorizationData;
    }

    //Make sure the current challenge objects are sent with the authorization object
    //Section 7.5.1
    public Authorization buildCurrentAuthorization(AuthorizationData authorizationData){
        ChallengePersistence challengePersistence = new ChallengePersistence();
        List<ChallengeData> challengeDataList = challengePersistence.getAllChallengesForAuthorization(authorizationData);

        Authorization authorization = authorizationData.getObject();

        authorization.setChallenges(challengeDataList.stream()
                .map(DataObject::getObject).toArray(Challenge[]::new));

        boolean isValid = challengeDataList.stream()
                .map(DataObject::getObject)
                .anyMatch(c -> c.getStatus().contentEquals(StatusType.VALID.toString()));

        if(isValid){
            authorization.setStatus(StatusType.VALID.toString());
            //TODO add expires
        }

        return authorization;
    }

    //Based off the directory, get the CA, which has the rules for how to build authorizations for identifiers
    public Optional<AuthorizationData> buildAuthorizationForIdentifier(Identifier identifier, DirectoryData directory){
        CertificateAuthority ca = new CertificateAuthorityService().getByName(directory.getMapsToCertificateAuthorityName());

        AuthorizationData authorizationData = buildNew(directory);
        Authorization authorization = authorizationData.getObject();
        authorization.setIdentifier(identifier);

        //Check if CA requires extra validation for identifier
        for(CAValidationRule rule : ca.getValidationRules()){
            //If type matches rule type
            if(identifier.getType().contentEquals(rule.getIdentifierType())){
                //And identifier base domain matches
                if(identifier.getValue().endsWith(rule.getBaseDomainName())){
                    ChallengeData challengeData = challengeProcessor.buildNew(directory);
                    //Needed for referencing later
                    challengeData.setAuthorizationId(authorizationData.getId());
                    Challenge challenge = challengeData.getObject();
                    challenge.setUrl(challengeData.buildUrl());
                    //TODO fill in and save


                    authorization.addChallenge(challenge);
                }
            }
        }

        //only save if a challenge was needed
        if(authorization.getChallenges().length > 0){
            new AuthorizationPersistence().save(authorizationData);
            return Optional.of(authorizationData);
        }else{
            return Optional.empty();
        }
    }
}
