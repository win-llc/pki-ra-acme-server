package com.winllc.acme.server.service.acme;

import com.nimbusds.jose.JWSObject;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.challenge.DnsChallenge;
import com.winllc.acme.server.challenge.HttpChallenge;
import com.winllc.acme.server.contants.ChallengeType;
import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.acme.server.model.AcmeURL;
import com.winllc.acme.server.model.acme.*;
import com.winllc.acme.server.model.data.AuthorizationData;
import com.winllc.acme.server.model.data.ChallengeData;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.persistence.AuthorizationPersistence;
import com.winllc.acme.server.persistence.ChallengePersistence;
import com.winllc.acme.server.process.AuthorizationProcessor;
import com.winllc.acme.server.process.ChallengeProcessor;
import com.winllc.acme.server.util.AppUtil;
import com.winllc.acme.server.util.PayloadAndAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

//Section 7.4.1
@RestController
public class AuthzService extends BaseService {

    @Autowired
    private AuthorizationPersistence authorizationPersistence;
    @Autowired
    private AuthorizationProcessor authorizationProcessor;
    @Autowired
    private ChallengeProcessor challengeProcessor;
    @Autowired
    private ChallengePersistence challengePersistence;

    @RequestMapping(value = "{directory}/new-authz", method = RequestMethod.POST, consumes = "application/jose+json")
    public ResponseEntity<?> newAuthz(HttpServletRequest request, @PathVariable String directory) {
        try {
            PayloadAndAccount<Identifier> payloadAndAccount = AppUtil.verifyJWSAndReturnPayloadForExistingAccount(request, Identifier.class);
            DirectoryData directoryData = payloadAndAccount.getDirectoryData();
            if(directoryData.isAllowPreAuthorization()) {
                Identifier identifier = payloadAndAccount.getPayload();

                if (serverWillingToIssueForIdentifier(identifier, directoryData, false)) {
                    Optional<AuthorizationData> authorizationOptional = authorizationProcessor.buildAuthorizationForIdentifier(identifier, directoryData);
                    if(authorizationOptional.isPresent()){
                        AuthorizationData authorizationData = authorizationOptional.get();
                        authorizationData.setAccountId(payloadAndAccount.getAccountData().getId());

                        authorizationData = authorizationPersistence.save(authorizationData);

                        return buildBaseResponseEntity(201, directoryData)
                                .body(authorizationData.getObject());
                    }else{
                        //TODO get proper problem type
                        ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
                        return buildBaseResponseEntity(500, directoryData)
                                .body(problemDetails);
                    }

                } else {
                    ProblemDetails problemDetails = new ProblemDetails(ProblemType.UNSUPPORTED_IDENTIFIER);
                    //TODO fill out problem details
                    return buildBaseResponseEntity(403, payloadAndAccount.getDirectoryData())
                            .body(problemDetails);
                }
            }else{
                //Pre-auth not allowed
                ProblemDetails problemDetails = new ProblemDetails(ProblemType.UNAUTHORIZED);
                return buildBaseResponseEntity(403, directoryData)
                        .body(problemDetails);
            }

        } catch (Exception e) {
            e.printStackTrace();
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
            problemDetails.setDetail(e.getMessage());

            return ResponseEntity.status(500)
                    .body(problemDetails);
        }
    }

    //Section 7.5
    @RequestMapping(value = "{directory}/authz/{id}", method = RequestMethod.POST, consumes = "application/jose+json", produces = "application/json")
    public ResponseEntity<?> authz(HttpServletRequest request, @PathVariable String id, @PathVariable String directory) {

        Optional<AuthorizationData> optionalAuthorizationData = authorizationPersistence.getById(id);

        if (optionalAuthorizationData.isPresent()) {
            AuthorizationData authorizationData = optionalAuthorizationData.get();

            try {
                PayloadAndAccount<Authorization> payloadAndAccount = AppUtil.verifyJWSAndReturnPayloadForExistingAccount(request, Authorization.class);
                Authorization authorization = payloadAndAccount.getPayload();

                //Section 7.5.2
                if (authorization.getStatus().contentEquals(StatusType.DEACTIVATED.toString())) {
                    authorizationData.getObject().setStatus(StatusType.DEACTIVATED.toString());
                    authorizationPersistence.save(authorizationData);

                    return buildBaseResponseEntity(200, payloadAndAccount.getDirectoryData())
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(authorizationData.getObject());
                }

                AuthorizationData refreshedAuthorization = authorizationProcessor.buildCurrentAuthorization(authorizationData);

                return buildBaseResponseEntity(200, payloadAndAccount.getDirectoryData())
                        .header("Link", "TODO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(refreshedAuthorization);

            } catch (Exception e) {
                e.printStackTrace();
                ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
                problemDetails.setDetail(e.getMessage());

                return ResponseEntity.status(500)
                        .body(problemDetails);
            }
        }else {
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
            return ResponseEntity.status(500)
                    .body(problemDetails);
        }
    }

    //Section 7.5.1
    @RequestMapping(value = "{directory}/chall/{id}", method = RequestMethod.POST, consumes = "application/jose+json", produces = "application/json")
    public ResponseEntity<?> challenge(HttpServletRequest request, @PathVariable String id, @PathVariable String directory) {
        Optional<ChallengeData> optionalChallengeData = challengePersistence.getById(id);
        AcmeURL acmeURL = new AcmeURL(request);
        DirectoryData directoryData = Application.directoryDataMap.get(acmeURL.getDirectoryIdentifier());
        if (optionalChallengeData.isPresent()) {
            ChallengeData challengeData = optionalChallengeData.get();
            Challenge challenge = challengeData.getObject();

            ChallengeType challengeType = ChallengeType.valueOf(challenge.getType());
            switch (challengeType) {
                case HTTP:
                    new HttpChallenge().verify(challengeData);
                    break;
                case DNS:
                    new DnsChallenge().verify(challengeData);
                    break;
            }
            //Get the current challenge and return 200
            Optional<ChallengeData> challengeDataOptional = challengePersistence.getById(challengeData.getId());
            return buildBaseResponseEntity(200, directoryData)
                    .body(challengeDataOptional.get().getObject());
        }else{
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
            problemDetails.setDetail("Could not find challenge");
            return buildBaseResponseEntity(500, directoryData)
                    .body(problemDetails);
        }
    }

    private boolean serverWillingToIssueForIdentifier(Identifier identifier, DirectoryData directoryData, boolean allowWildcards) {
        CertificateAuthority ca = Application.availableCAs.get(directoryData.getMapsToCertificateAuthorityName());

        if(!allowWildcards && identifier.getValue().startsWith("*")){
            return false;
        }else {
            return ca.canIssueToIdentifier(identifier);
        }
    }

}
