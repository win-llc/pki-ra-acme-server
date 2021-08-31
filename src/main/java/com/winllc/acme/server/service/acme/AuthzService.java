package com.winllc.acme.server.service.acme;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.acme.common.constants.ProblemType;
import com.winllc.acme.common.constants.StatusType;
import com.winllc.acme.common.model.AcmeJWSObject;
import com.winllc.acme.common.model.acme.Authorization;
import com.winllc.acme.common.model.acme.Challenge;
import com.winllc.acme.common.model.acme.Identifier;
import com.winllc.acme.common.model.acme.ProblemDetails;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.AuthorizationData;
import com.winllc.acme.common.model.data.ChallengeData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.exceptions.InternalServerException;
import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.acme.server.external.ExternalAccountProvider;
import com.winllc.acme.server.persistence.AuthorizationPersistence;
import com.winllc.acme.server.persistence.ChallengePersistence;
import com.winllc.acme.server.service.internal.CertificateAuthorityService;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import com.winllc.acme.server.service.internal.ExternalAccountProviderService;
import com.winllc.acme.server.transaction.*;
import com.winllc.acme.server.util.NonceUtil;
import com.winllc.acme.server.util.PayloadAndAccount;
import com.winllc.acme.server.util.SecurityValidatorUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//Section 7.4.1
@RestController
public class AuthzService extends BaseService {

    private static final Logger log = LogManager.getLogger(AuthzService.class);

    private final AuthorizationPersistence authorizationPersistence;
    private final ChallengePersistence challengePersistence;
    private final DirectoryDataService directoryDataService;
    private final SecurityValidatorUtil securityValidatorUtil;
    private final CertificateAuthorityService certificateAuthorityService;
    private final ExternalAccountProviderService externalAccountProviderService;

    private final AcmeTransactionManagement acmeTransactionManagement;

    protected AuthzService(NonceUtil nonceUtil, AuthorizationPersistence authorizationPersistence,
                           ChallengePersistence challengePersistence, DirectoryDataService directoryDataService,
                           SecurityValidatorUtil securityValidatorUtil, CertificateAuthorityService certificateAuthorityService,
                           ExternalAccountProviderService externalAccountProviderService, AcmeTransactionManagement acmeTransactionManagement) {
        super(nonceUtil);
        this.authorizationPersistence = authorizationPersistence;
        this.challengePersistence = challengePersistence;
        this.directoryDataService = directoryDataService;
        this.securityValidatorUtil = securityValidatorUtil;
        this.certificateAuthorityService = certificateAuthorityService;
        this.externalAccountProviderService = externalAccountProviderService;
        this.acmeTransactionManagement = acmeTransactionManagement;
    }

    @RequestMapping(value = "{directory}/new-authz", method = RequestMethod.POST, consumes = "application/jose+json")
    public ResponseEntity<?> newAuthz(HttpServletRequest request, @PathVariable String directory) {
        log.info("start new authz");
        try {
            PayloadAndAccount<Identifier> payloadAndAccount =
                    securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(request, Identifier.class);
            DirectoryData directoryData = payloadAndAccount.getDirectoryData();
            if (directoryData.isAllowPreAuthorization()) {
                Identifier identifier = payloadAndAccount.getPayload();

               boolean identifierPreApproved = checkIdentifierAllowedPreAuthz(payloadAndAccount);

                PreAuthzTransaction preAuthzTransaction = acmeTransactionManagement
                        .startNewPreAuthz(payloadAndAccount.getAccountData(), payloadAndAccount.getDirectoryData());

                preAuthzTransaction.start(identifier, identifierPreApproved);
                AuthorizationData authorizationData = preAuthzTransaction.getData();

                return buildBaseResponseEntity(201, directoryData)
                        .header("Location", authorizationData.buildUrl(Application.baseURL))
                        .body(authorizationData.getObject());
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

    //Get identifiers associated with account that are marked for pre-authz
    private boolean checkIdentifierAllowedPreAuthz(PayloadAndAccount<Identifier> payloadAndAccount){
        List<String> preAuthzIdentifiersForDirectory = getPreAuthzIdentifiersForDirectory(payloadAndAccount.getDirectoryData(), payloadAndAccount.getAccountData());

        return preAuthzIdentifiersForDirectory.contains(payloadAndAccount.getPayload().getValue());
    }

    private List<String> getPreAuthzIdentifiersForDirectory(DirectoryData directoryData, AccountData accountData){
        List<String> allowedIdentifiers = new ArrayList<>();
        String accountProviderName = directoryData.getExternalAccountProviderName();
        if(StringUtils.isNotBlank(accountProviderName)) {
            ExternalAccountProvider accountProvider
                    = externalAccountProviderService.findByName(directoryData.getExternalAccountProviderName());

            if(accountProvider != null) {
                try {
                    List<String> preAuthorizationIdentifiers
                            = accountProvider.getPreAuthorizationIdentifiers(accountData.getEabKeyIdentifier());
                    allowedIdentifiers.addAll(preAuthorizationIdentifiers);
                } catch (InternalServerException e) {
                    log.error("Could not retrieve pre-authz", e);
                }
            }
        }

        return allowedIdentifiers;
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

                //if content is blank, normal authz workflow
                if (jwsObject.getPayload().toString().contentEquals("")) {
                    AbstractTransaction transaction;
                    if(authorizationData.getPreAuthz()){
                        transaction = acmeTransactionManagement.getTransaction(
                                authorizationData.getTransactionId(), PreAuthzTransaction.class);
                    }else{
                        transaction = acmeTransactionManagement.getTransaction(
                                authorizationData.getTransactionId(), CertIssuanceTransaction.class);
                    }

                    Authorization authorization = authorizationData.getObject();

                    log.info("Returning current authorization: " + new ObjectMapper().writeValueAsString(authorization));
                    if (authorization.getStatus().equals(StatusType.PENDING.toString())) {
                        return buildBaseResponseEntityWithRetryAfter(200, transaction.getTransactionContext().getDirectoryData(), 20)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(authorization);
                    } else {
                        return buildBaseResponseEntity(200, transaction.getTransactionContext().getDirectoryData())
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(authorization);
                    }

                } else {
                    //If payload contains a status, most likely to deactivate
                    PayloadAndAccount payloadAndAccount = securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(request, authorizationData.getAccountId(), Authorization.class);
                    Authorization authorization = (Authorization) payloadAndAccount.getPayload();

                    //Section 7.5.2
                    if (authorization.getStatus().contentEquals(StatusType.DEACTIVATED.toString())) {
                        authorizationData.getObject().markDeactivated();
                        authorizationPersistence.save(authorizationData);

                        log.info("Setting authorization to deactivated: " + authorizationData);

                        return buildBaseResponseEntity(200, payloadAndAccount.getDirectoryData())
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(authorizationData.getObject());
                    }else{
                        ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
                        problemDetails.setDetail("Unexpected data in payload");
                        throw new AcmeServerException(problemDetails);
                    }
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

            AuthorizationData authorizationData = authorizationPersistence.findById(updatedChallengeData.getAuthorizationId()).get();

            AuthorizationTransaction authorizationTransaction;
            if(authorizationData.getPreAuthz()){
                authorizationTransaction = acmeTransactionManagement.getTransaction(updatedChallengeData.getTransactionId(),
                        PreAuthzTransaction.class);
            }else{
                authorizationTransaction = acmeTransactionManagement.getTransaction(
                        updatedChallengeData.getTransactionId(), CertIssuanceTransaction.class);
            }

            updatedChallengeData = authorizationTransaction.retrieveChallengeData(id).getData();

            Challenge challenge = updatedChallengeData.getObject();

            if (challenge.getStatus().equals(StatusType.PROCESSING.toString()) ||
                    challenge.getStatus().equals(StatusType.PENDING.toString())) {

                authorizationTransaction.attemptChallenge(updatedChallengeData.getId());

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
                        .header("Link", "<"+authorizationData.buildUrl(Application.baseURL)+">;rel=\"up\"")
                        .body(challenge);
            } else {
                return buildBaseResponseEntity(200, directoryData)
                        .header("Link", "<"+authorizationData.buildUrl(Application.baseURL)+">;rel=\"up\"")
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
