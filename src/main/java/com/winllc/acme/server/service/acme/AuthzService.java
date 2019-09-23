package com.winllc.acme.server.service.acme;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.challenge.DnsChallenge;
import com.winllc.acme.server.challenge.HttpChallenge;
import com.winllc.acme.server.contants.ChallengeType;
import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.acme.server.model.AcmeJWSObject;
import com.winllc.acme.server.model.acme.*;
import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.AuthorizationData;
import com.winllc.acme.server.model.data.ChallengeData;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.persistence.AuthorizationPersistence;
import com.winllc.acme.server.persistence.ChallengePersistence;
import com.winllc.acme.server.process.AuthorizationProcessor;
import com.winllc.acme.server.service.internal.CertificateAuthorityService;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import com.winllc.acme.server.util.SecurityValidatorUtil;
import com.winllc.acme.server.util.PayloadAndAccount;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger log = LogManager.getLogger(AuthzService.class);

    @Autowired
    private AuthorizationPersistence authorizationPersistence;
    @Autowired
    private AuthorizationProcessor authorizationProcessor;
    @Autowired
    private ChallengePersistence challengePersistence;
    @Autowired
    private HttpChallenge httpChallenge;
    @Autowired
    private DnsChallenge dnsChallenge;
    @Autowired
    private DirectoryDataService directoryDataService;
    @Autowired
    private SecurityValidatorUtil securityValidatorUtil;
    @Autowired
    private CertificateAuthorityService certificateAuthorityService;

    @RequestMapping(value = "{directory}/new-authz", method = RequestMethod.POST, consumes = "application/jose+json")
    public ResponseEntity<?> newAuthz(HttpServletRequest request, @PathVariable String directory) {
        try {
            PayloadAndAccount<Identifier> payloadAndAccount = securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(request, Identifier.class);
            DirectoryData directoryData = payloadAndAccount.getDirectoryData();
            if(directoryData.isAllowPreAuthorization()) {
                Identifier identifier = payloadAndAccount.getPayload();

                if (serverWillingToIssueForIdentifier(identifier, directoryData, payloadAndAccount.getAccountData(),false)) {
                    Optional<AuthorizationData> authorizationOptional = authorizationProcessor.buildAuthorizationForIdentifier(identifier, payloadAndAccount, null);
                    if(authorizationOptional.isPresent()){
                        AuthorizationData authorizationData = authorizationOptional.get();

                        authorizationData = authorizationPersistence.save(authorizationData);

                        return buildBaseResponseEntity(201, directoryData)
                                .body(authorizationData.getObject());
                    }else{
                        ProblemDetails problemDetails = new ProblemDetails(ProblemType.REJECTED_IDENTIFIER);
                        return buildBaseResponseEntity(403, directoryData)
                                .body(problemDetails);
                    }

                } else {
                    ProblemDetails problemDetails = new ProblemDetails(ProblemType.UNSUPPORTED_IDENTIFIER);
                    problemDetails.setDetail("Will not issue to: "+identifier);
                    return buildBaseResponseEntity(403, payloadAndAccount.getDirectoryData())
                            .body(problemDetails);
                }
            }else{
                //Pre-auth not allowed
                ProblemDetails problemDetails = new ProblemDetails(ProblemType.UNAUTHORIZED);

                log.error(problemDetails);

                return buildBaseResponseEntity(403, directoryData)
                        .body(problemDetails);
            }

        } catch (Exception e) {
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
            problemDetails.setDetail(e.getMessage());

            log.error(problemDetails, e);

            return ResponseEntity.status(500)
                    .body(problemDetails);
        }
    }

    //Section 7.5
    @RequestMapping(value = "{directory}/authz/{id}", method = RequestMethod.POST, consumes = "application/jose+json", produces = "application/json")
    public ResponseEntity<?> authz(HttpServletRequest request, @PathVariable String id, @PathVariable String directory) {
        Optional<AuthorizationData> optionalAuthorizationData = authorizationPersistence.findById(id);

        if (optionalAuthorizationData.isPresent()) {
            AuthorizationData authorizationData = optionalAuthorizationData.get();

            try {
                AcmeJWSObject jwsObject = SecurityValidatorUtil.getJWSObjectFromHttpRequest(request);
                PayloadAndAccount payloadAndAccount;
                if (jwsObject.getPayload().toString().contentEquals("")) {
                    payloadAndAccount = securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(jwsObject, request, authorizationData.getAccountId(), String.class);
                }else{
                    payloadAndAccount = securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(request, authorizationData.getAccountId(), Authorization.class);
                    Authorization authorization = (Authorization) payloadAndAccount.getPayload();

                    //Section 7.5.2
                    if (authorization.getStatus().contentEquals(StatusType.DEACTIVATED.toString())) {
                        authorizationData.getObject().setStatus(StatusType.DEACTIVATED.toString());
                        authorizationPersistence.save(authorizationData);

                        log.info("Setting authorization to deactivated: "+authorizationData);

                        return buildBaseResponseEntity(200, payloadAndAccount.getDirectoryData())
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(authorizationData.getObject());
                    }
                }

                AuthorizationData refreshedAuthorization = authorizationProcessor.buildCurrentAuthorization(authorizationData);

                log.info("Returning current authorization: "+refreshedAuthorization);

                return buildBaseResponseEntity(200, payloadAndAccount.getDirectoryData())
                        .header("Link", "TODO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(refreshedAuthorization.getObject());

            } catch (Exception e) {
                ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
                problemDetails.setDetail(e.getMessage());

                log.error(problemDetails, e);

                return ResponseEntity.status(500)
                        .body(problemDetails);
            }
        }else {
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
            log.error(problemDetails);

            return ResponseEntity.status(500)
                    .body(problemDetails);
        }
    }

    //Section 7.5.1
    @RequestMapping(value = "{directory}/chall/{id}", method = RequestMethod.POST, consumes = "application/jose+json", produces = "application/json")
    public ResponseEntity<?> challenge(HttpServletRequest request, @PathVariable String id, @PathVariable String directory) {
        Optional<ChallengeData> optionalChallengeData = challengePersistence.findById(id);
        DirectoryData directoryData = directoryDataService.findByName(directory);

        AcmeJWSObject jwsObjectFromHttpRequest;
        try {
            jwsObjectFromHttpRequest = SecurityValidatorUtil.getJWSObjectFromHttpRequest(request);
            log.info(jwsObjectFromHttpRequest);
        } catch (AcmeServerException e) {
            e.printStackTrace();
        }
        if (optionalChallengeData.isPresent()) {
            ChallengeData challengeData = optionalChallengeData.get();
            Challenge challenge = challengeData.getObject();
            Optional<AuthorizationData> authOptional = authorizationPersistence.findById(challengeData.getAuthorizationId());

            ChallengeType challengeType = ChallengeType.getValue(challenge.getType());
            switch (challengeType) {
                case HTTP:
                    httpChallenge.verify(challengeData);
                    break;
                case DNS:
                    dnsChallenge.verify(challengeData);
                    break;
            }
            //Get the current challenge and return 200
            Optional<ChallengeData> challengeDataOptional = challengePersistence.findById(challengeData.getId());
            return buildBaseResponseEntity(200, directoryData)
                    .header("Link", "<"+authOptional.get().buildUrl()+">;rel=\"up\"")
                    .body(challengeDataOptional.get().getObject());
        }else{
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
            problemDetails.setDetail("Could not find challenge");
            log.error(problemDetails);

            return buildBaseResponseEntity(500, directoryData)
                    .body(problemDetails);
        }
    }

    private boolean serverWillingToIssueForIdentifier(Identifier identifier, DirectoryData directoryData, AccountData accountData, boolean allowWildcards) {
        CertificateAuthority ca = certificateAuthorityService.getByName(directoryData.getMapsToCertificateAuthorityName());

        if(!allowWildcards && identifier.getValue().startsWith("*")){
            return false;
        }else {
            return ca.canIssueToIdentifier(identifier, accountData);
        }
    }

}
