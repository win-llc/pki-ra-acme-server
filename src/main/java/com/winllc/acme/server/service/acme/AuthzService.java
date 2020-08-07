package com.winllc.acme.server.service.acme;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.challenge.DnsChallenge;
import com.winllc.acme.server.challenge.HttpChallenge;
import com.winllc.acme.common.contants.ProblemType;
import com.winllc.acme.common.contants.StatusType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.acme.common.model.AcmeJWSObject;
import com.winllc.acme.common.model.acme.*;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.AuthorizationData;
import com.winllc.acme.common.model.data.ChallengeData;
import com.winllc.acme.common.model.data.DirectoryData;
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
import java.util.stream.Stream;

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
    private DirectoryDataService directoryDataService;
    @Autowired
    private SecurityValidatorUtil securityValidatorUtil;
    @Autowired
    private CertificateAuthorityService certificateAuthorityService;
    @Autowired
    private HttpChallenge httpChallenge;
    @Autowired
    private DnsChallenge dnsChallenge;

    @RequestMapping(value = "{directory}/new-authz", method = RequestMethod.POST, consumes = "application/jose+json")
    public ResponseEntity<?> newAuthz(HttpServletRequest request, @PathVariable String directory) {
        log.info("start new authz");
        try {
            PayloadAndAccount<Identifier> payloadAndAccount = securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(request, Identifier.class);
            DirectoryData directoryData = payloadAndAccount.getDirectoryData();
            if (directoryData.isAllowPreAuthorization()) {
                Identifier identifier = payloadAndAccount.getPayload();

                if (serverWillingToIssueForIdentifier(identifier, directoryData, payloadAndAccount.getAccountData())) {
                    Optional<AuthorizationData> authorizationOptional = authorizationProcessor.buildAuthorizationForIdentifier(identifier, payloadAndAccount, null);
                    if (authorizationOptional.isPresent()) {
                        AuthorizationData authorizationData = authorizationOptional.get();

                        authorizationData = authorizationPersistence.save(authorizationData);

                        return buildBaseResponseEntity(201, directoryData)
                                .header("Location", authorizationData.buildUrl(Application.baseURL))
                                .body(authorizationData.getObject());
                    } else {
                        ProblemDetails problemDetails = new ProblemDetails(ProblemType.REJECTED_IDENTIFIER);
                        return buildBaseResponseEntity(403, directoryData)
                                .body(problemDetails);
                    }

                } else {
                    ProblemDetails problemDetails = new ProblemDetails(ProblemType.UNSUPPORTED_IDENTIFIER);
                    problemDetails.setDetail("Will not issue to: " + identifier);
                    return buildBaseResponseEntity(403, payloadAndAccount.getDirectoryData())
                            .body(problemDetails);
                }
            } else {
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
    @RequestMapping(value = "{directory}/authz/{id}", consumes = "application/jose+json", produces = "application/json")
    public ResponseEntity<?> authz(HttpServletRequest request, @PathVariable String id, @PathVariable String directory) throws AcmeServerException {
        log.info("STEP TWO - GET CHALLENGES");
        Optional<AuthorizationData> optionalAuthorizationData = authorizationPersistence.findById(id);

        if (optionalAuthorizationData.isPresent()) {
            AuthorizationData authorizationData = optionalAuthorizationData.get();

            try {
                AcmeJWSObject jwsObject = SecurityValidatorUtil.getJWSObjectFromHttpRequest(request);
                PayloadAndAccount payloadAndAccount;
                if (jwsObject.getPayload().toString().contentEquals("")) {
                    payloadAndAccount = securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(jwsObject, request.getRequestURL().toString(), authorizationData.getAccountId(), String.class);
                } else {
                    payloadAndAccount = securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(request, authorizationData.getAccountId(), Authorization.class);
                    Authorization authorization = (Authorization) payloadAndAccount.getPayload();

                    //Section 7.5.2
                    if (authorization.getStatus().contentEquals(StatusType.DEACTIVATED.toString())) {
                        authorizationData.getObject().markDeactivated();
                        authorizationPersistence.save(authorizationData);

                        log.info("Setting authorization to deactivated: " + authorizationData);

                        return buildBaseResponseEntity(200, payloadAndAccount.getDirectoryData())
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(authorizationData.getObject());
                    }
                }

                AuthorizationData refreshedAuthorization = authorizationProcessor.buildCurrentAuthorization(authorizationData);

                log.info("Returning current authorization: " + refreshedAuthorization);

                ObjectMapper mapper = new ObjectMapper();

                if(refreshedAuthorization.getObject().getChallenges() != null) {
                    Stream.of(refreshedAuthorization.getObject().getChallenges())
                            .forEach(c -> c.setStatus(null));
                }

                String jsonObj = mapper.writeValueAsString(refreshedAuthorization.getObject());

                if (refreshedAuthorization.getObject().getStatus().equals(StatusType.PENDING.toString())) {
                    return buildBaseResponseEntityWithRetryAfter(200, payloadAndAccount.getDirectoryData(), 20)
                            //.header("Link", "TODO")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jsonObj);
                } else {
                    return buildBaseResponseEntity(200, payloadAndAccount.getDirectoryData())
                            //.header("Link", "TODO")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jsonObj);
                }

            } catch (Exception e) {
                ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
                problemDetails.setDetail(e.getMessage());

                log.error(problemDetails, e);

                throw new AcmeServerException(problemDetails);
            }
        } else {
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
            log.error(problemDetails);

            throw new AcmeServerException(problemDetails);
        }
    }

    //Section 7.5.1
    @RequestMapping(value = "{directory}/chall/{id}", method = RequestMethod.POST, consumes = "application/jose+json", produces = "application/json")
    public ResponseEntity<?> challenge(@PathVariable String id, @PathVariable String directory) throws Exception {
        log.info("STEP THREE - POST CHALLENGES");
        Optional<ChallengeData> optionalChallengeData = challengePersistence.findById(id);
        DirectoryData directoryData = directoryDataService.findByName(directory);

        if (optionalChallengeData.isPresent()) {
            ChallengeData updatedChallengeData = optionalChallengeData.get();
            Challenge challenge = updatedChallengeData.getObject();

            if (challenge.getStatus().equals(StatusType.PROCESSING.toString()) ||
                    challenge.getStatus().equals(StatusType.PENDING.toString())) {

                if (challenge.getStatus().equals(StatusType.PENDING.toString())) {
                    switch (challenge.getType()) {
                        case "http-01":
                            httpChallenge.verify(updatedChallengeData);
                            break;
                        case "dns-01":
                            dnsChallenge.verify(updatedChallengeData);
                            break;
                    }
                }

                ProblemDetails pd = new ProblemDetails(ProblemType.ORDER_NOT_READY);
                pd.setStatus(200);
                pd.setSubproblems(null);
                pd.setDetail("Challenge not ready");
                //challenge.setError(pd);
                //challenge.setStatus(StatusType.PROCESSING.toString());

                ObjectMapper objectMapper = new ObjectMapper();
                String jsonObj = objectMapper.writeValueAsString(challenge);

                log.info("Challenge processing: " + jsonObj);

                return buildBaseResponseEntityWithRetryAfter(200, directoryData, 10)
                        //.header("Link", "<"+authOptional.get().buildUrl()+">;rel=\"up\"")
                        .body(challenge);
            } else {
                return buildBaseResponseEntity(200, directoryData)
                        //.header("Link", "<"+authOptional.get().buildUrl()+">;rel=\"up\"")
                        .body(updatedChallengeData.getObject());
            }

        } else {
            log.error("Could not find challenge");
            throw new AcmeServerException(ProblemType.SERVER_INTERNAL);
        }
    }

    //7.4.1 wildcards not allowed
    private boolean serverWillingToIssueForIdentifier(Identifier identifier, DirectoryData directoryData, AccountData accountData) throws AcmeServerException {
        CertificateAuthority ca = certificateAuthorityService.getByName(directoryData.getMapsToCertificateAuthorityName());

        if (identifier.getValue().startsWith("*")) {
            return false;
        } else {
            return ca.canIssueToIdentifier(identifier, accountData, directoryData);
        }
    }

}
