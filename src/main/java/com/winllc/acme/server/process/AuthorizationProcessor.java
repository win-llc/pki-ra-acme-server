package com.winllc.acme.server.process;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.exceptions.InternalServerException;
import com.winllc.acme.server.external.CAValidationRule;
import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.acme.server.model.acme.Authorization;
import com.winllc.acme.server.model.acme.Challenge;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.acme.Identifier;
import com.winllc.acme.server.model.data.*;
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
    private AuthorizationPersistence authorizationPersistence;
    private OrderProcessor orderProcessor;

    @Override
    public AuthorizationData buildNew(DirectoryData directoryData) {
        Authorization authorization = new Authorization();
        authorization.setStatus(StatusType.PENDING.toString());

        AuthorizationData authorizationData = new AuthorizationData(authorization, directoryData);

        return authorizationData;
    }

    //Make sure the current challenge objects are sent with the authorization object
    //Section 7.5.1
    public AuthorizationData buildCurrentAuthorization(AuthorizationData authorizationData){
        if(authorizationData.getObject().isExpired()){
            authorizationData.getObject().setStatus(StatusType.EXPIRED.toString());
            authorizationPersistence.save(authorizationData);
        }

        return authorizationData;
    }

    //Based off the directory, get the CA, which has the rules for how to build authorizations for identifiers
    public Optional<AuthorizationData> buildAuthorizationForIdentifier(Identifier identifier, DirectoryData directory){
        CertificateAuthority ca = Application.availableCAs.get(directory.getMapsToCertificateAuthorityName());

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

    //If a challenge is marked valid, authorization should be marked valid, unless it's expired
    public AuthorizationData challengeMarkedValid(String authorizationId) throws InternalServerException {
        Optional<AuthorizationData> authorizationDataOptional = authorizationPersistence.getById(authorizationId);
        if(authorizationDataOptional.isPresent()){
            AuthorizationData authorizationData = authorizationDataOptional.get();

            if(authorizationData.getObject().isExpired()){
                authorizationData.getObject().setStatus(StatusType.EXPIRED.toString());
                authorizationData = authorizationPersistence.save(authorizationData);
            }else{
                authorizationData = markValid(authorizationData);
            }

            return authorizationData;
        }

        throw new InternalServerException("Could not find authorization");
    }

    //Only mark valid if currently in pending state
    public AuthorizationData markValid(AuthorizationData authorizationData) throws InternalServerException {
        if(authorizationData.getObject().getStatus().equalsIgnoreCase(StatusType.PENDING.toString())){
            authorizationData.getObject().setStatus(StatusType.VALID.toString());
            authorizationData = authorizationPersistence.save(authorizationData);

            //If authorization marked valid, let order know
            orderProcessor.authorizationMarkedValid(authorizationData.getOrderId());
        }
        return authorizationData;
    }

    //Check if any authorizations are expired and update before returning
    public List<AuthorizationData> getCurrentAuthorizationsForOrder(OrderData orderData){
        List<AuthorizationData> authorizationDataList = authorizationPersistence.getAllAuthorizationsForOrder(orderData);
        for(AuthorizationData authorizationData : authorizationDataList){
            if(authorizationData.getObject().isExpired()){
                authorizationData.getObject().setStatus(StatusType.EXPIRED.toString());
                authorizationPersistence.save(authorizationData);
            }
        }

        return authorizationDataList;
    }
}
