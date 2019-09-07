package com.winllc.acme.server.process;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.contants.ChallengeType;
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
import com.winllc.acme.server.util.PayloadAndAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
@Component
public class AuthorizationProcessor implements AcmeDataProcessor<AuthorizationData> {

    @Autowired
    private ChallengeProcessor challengeProcessor;
    @Autowired
    private ChallengePersistence challengePersistence;
    @Autowired
    private AuthorizationPersistence authorizationPersistence;
    @Autowired
    private OrderProcessor orderProcessor;
    @Autowired
    private CertificateAuthorityService certificateAuthorityService;

    @Override
    public AuthorizationData buildNew(DirectoryData directoryData) {
        Authorization authorization = new Authorization();
        authorization.setStatus(StatusType.PENDING.toString());

        AuthorizationData authorizationData = new AuthorizationData(authorization, directoryData);

        return authorizationData;
    }

    public AuthorizationData buildNew(PayloadAndAccount payloadAndAccount) {
        AuthorizationData authorization = buildNew(payloadAndAccount.getDirectoryData());

        authorization.setAccountId(payloadAndAccount.getAccountData().getId());

        return authorization;
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
    public Optional<AuthorizationData> buildAuthorizationForIdentifier(Identifier identifier, PayloadAndAccount payloadAndAccount){
        DirectoryData directory = payloadAndAccount.getDirectoryData();
        CertificateAuthority ca = certificateAuthorityService.getByName(directory.getMapsToCertificateAuthorityName());

        AuthorizationData authorizationData = buildNew(payloadAndAccount);
        Authorization authorization = authorizationData.getObject();
        authorization.setIdentifier(identifier);
        authorization.willExpireInMinutes(60);

        if(ca.canIssueToIdentifier(identifier, payloadAndAccount.getAccountData())){

            List<ChallengeType> identifierChallengeRequirements = ca.getIdentifierChallengeRequirements(identifier, payloadAndAccount.getAccountData());

            //Check if CA requires extra validation for identifier
            if(identifierChallengeRequirements != null){
                for(ChallengeType challengeType : identifierChallengeRequirements){
                    ChallengeData challengeData = challengeProcessor.buildNew(directory);
                    //Needed for referencing later
                    challengeData.setAuthorizationId(authorizationData.getId());
                    Challenge challenge = challengeData.getObject();
                    challenge.setUrl(challengeData.buildUrl());
                    challenge.setType(challengeType.toString());

                    challengePersistence.save(challengeData);

                    authorization.addChallenge(challenge);
                }
            }
        }

        //if not challenges needed, mark as valid
        if(authorization.getChallenges().length == 0){
            authorization.setStatus(StatusType.VALID.toString());
        }

        authorizationData = authorizationPersistence.save(authorizationData);
        return Optional.of(authorizationData);
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
