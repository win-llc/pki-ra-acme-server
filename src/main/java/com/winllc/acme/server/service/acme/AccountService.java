package com.winllc.acme.server.service.acme;

import com.nimbusds.jose.jwk.JWK;
import com.winllc.acme.common.constants.ProblemType;
import com.winllc.acme.common.constants.StatusType;
import com.winllc.acme.common.model.AcmeJWSObject;
import com.winllc.acme.common.model.acme.ProblemDetails;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.common.model.requestresponse.AccountRequest;
import com.winllc.acme.common.model.requestresponse.KeyChangeRequest;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.persistence.AccountPersistence;
import com.winllc.acme.server.process.AccountProcessor;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import com.winllc.acme.server.util.NonceUtil;
import com.winllc.acme.server.util.PayloadAndAccount;
import com.winllc.acme.server.util.SecurityValidatorUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.Optional;

//Section 7.3
@RestController
public class AccountService extends BaseService {

    private static final Logger log = LogManager.getLogger(AccountService.class);

    private final AccountPersistence accountPersistence;
    private final AccountProcessor accountProcessor;
    private final DirectoryDataService directoryDataService;
    private final SecurityValidatorUtil securityValidatorUtil;

    protected AccountService(NonceUtil nonceUtil, AccountPersistence accountPersistence,
                             AccountProcessor accountProcessor, DirectoryDataService directoryDataService,
                             SecurityValidatorUtil securityValidatorUtil) {
        super(nonceUtil);
        this.accountPersistence = accountPersistence;
        this.accountProcessor = accountProcessor;
        this.directoryDataService = directoryDataService;
        this.securityValidatorUtil = securityValidatorUtil;
    }

    //Section 7.3
    @RequestMapping(value = "{directory}/new-account", method = RequestMethod.POST, consumes = "application/jose+json")
    public ResponseEntity<?> request(HttpServletRequest request, @PathVariable String directory) throws Exception {
        log.info("new-account request");

        AcmeJWSObject jwsObject = SecurityValidatorUtil.getJWSObjectFromHttpRequest(request);

        DirectoryData directoryData = directoryDataService.findByName(directory);

        //If JWS invalid, don't proceed
        if (!SecurityValidatorUtil.verifyJWS(jwsObject)) {
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.MALFORMED, 500);

            throw new AcmeServerException(problemDetails);
        }

        AccountRequest accountRequest = SecurityValidatorUtil.getPayloadFromJWSObject(jwsObject, AccountRequest.class);

        //Section 7.3.1 Paragraph 2
        if (accountRequest.getOnlyReturnExisting() != null && accountRequest.getOnlyReturnExisting()) {
            AccountData accountData = accountProcessor.processReturnExisting(jwsObject);

            return buildBaseResponseEntity(200, directoryData)
                    .body(accountData.getObject());
        }else{
            //If terms of service exist, ensure client has agreed
            AccountData accountData = accountProcessor.processCreateNewAccount(accountRequest, directoryData, jwsObject);

            return buildBaseResponseEntity(201, directoryData)
                    .header("Location", accountData.buildUrl(Application.baseURL))
                    .body(accountData.getObject());
        }
    }

    //Section 7.3.2
    @RequestMapping(value = "{directory}/acct/{id}", method = RequestMethod.POST, consumes = "application/jose+json")
    public ResponseEntity<?> update(@PathVariable String id, HttpServletRequest request, @PathVariable String directory) throws Exception {
        log.info("update account id: "+id);
        PayloadAndAccount<AccountRequest> payloadAndAccount = securityValidatorUtil
                .verifyJWSAndReturnPayloadForExistingAccount(request, id, AccountRequest.class);

        AccountRequest accountRequest = payloadAndAccount.getPayload();
        DirectoryData directoryData = payloadAndAccount.getDirectoryData();

        AccountData accountData = payloadAndAccount.getAccountData();

        if(!accountData.getObject().getStatus().equalsIgnoreCase(StatusType.DEACTIVATED.toString())) {
            //Section 7.3.6
            if (StringUtils.isNotBlank(accountRequest.getStatus())) {

                boolean updated = false;
                if(accountRequest.getStatus().contentEquals(StatusType.DEACTIVATED.toString())) {
                    accountData = accountProcessor.deactivateAccount(accountData, directoryData);
                    updated = true;
                }else if(accountRequest.getStatus().contentEquals(StatusType.REVOKED.toString())){
                    accountData = accountProcessor.accountRevoke(accountData, directoryData);
                    updated = true;
                }

                if(updated) {
                    return buildBaseResponseEntity(200, payloadAndAccount.getDirectoryData())
                            .body(accountData.getObject());
                }
            }

            //Section 7.3.2 update the contacts
            validateAccountRequest(accountRequest);
            accountData.getObject().setContact(accountRequest.getContact());

            //Section 7.3.3
            if (!checkChangeInTermsOfService(accountData, directoryData)) {
                ProblemDetails problemDetails = validateContactField(accountRequest);
                if (problemDetails == null) {
                    accountData.getObject().setContact(accountRequest.getContact());

                    accountData = accountPersistence.save(accountData);

                    return buildBaseResponseEntity(200, payloadAndAccount.getDirectoryData())
                            .body(accountData.getObject());
                }else{
                    return buildBaseResponseEntity(500, directoryData)
                            .body(problemDetails);
                }
            } else {
                ProblemDetails problemDetails = new ProblemDetails(ProblemType.USER_ACTION_REQUIRED);
                problemDetails.setDetail("Terms of service have changed");
                problemDetails.setInstance(directoryData.getObject().getMeta().getTermsOfService());

                return buildBaseResponseEntity(403, payloadAndAccount.getDirectoryData())
                        .header("Link", "<"+directoryData.getObject().getMeta().getTermsOfService()+">;rel=\"terms-of-service")
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body(problemDetails);
            }
        }else{
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.UNAUTHORIZED);
            problemDetails.setDetail("Account deactivated, can't update");

            log.error(problemDetails);

            return buildBaseResponseEntity(403, payloadAndAccount.getDirectoryData())
                    .body(problemDetails);

        }
    }

    //Section 7.3.5
    @RequestMapping(value = "{directory}/key-change", method = RequestMethod.POST, consumes = "application/jose+json")
    public ResponseEntity<?> keyRollover(HttpServletRequest httpRequest, @PathVariable String directory) throws Exception {
        log.info("Processing key change");

        PayloadAndAccount<AcmeJWSObject> payloadAndAccount = securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(httpRequest, AcmeJWSObject.class);
        AccountData accountData = payloadAndAccount.getAccountData();
        String newKey = payloadAndAccount.getPayload().getHeader().getJWK().toString();

        if (validateJWSForKeyChange(payloadAndAccount)) {
            accountData.setJwk(newKey);

            accountData = accountPersistence.save(accountData);

            log.info("key-change success for "+accountData);

            return buildBaseResponseEntity(200, payloadAndAccount.getDirectoryData())
                    .body(accountData.getObject());
        } else {
            Optional<AccountData> optionalAccount = accountPersistence.findFirstByJwkEquals(newKey);
            //If account already exists, return conflict error
            if(optionalAccount.isPresent()){
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", payloadAndAccount.getAccountData().buildUrl(Application.baseURL));

                ProblemDetails problemDetails = new ProblemDetails(ProblemType.UNAUTHORIZED);

                log.error(problemDetails);

                return buildBaseResponseEntity(409, payloadAndAccount.getDirectoryData())
                        .headers(headers)
                        .body(problemDetails);
            }else{
                //Key rollover failed for another reason
                ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);

                return buildBaseResponseEntity(500, payloadAndAccount.getDirectoryData())
                        .body(problemDetails);
            }
        }
    }

    //Section 7.3.5
    //Must verify the inner JWS Object before updating account key
    private boolean validateJWSForKeyChange(PayloadAndAccount<AcmeJWSObject> payloadAndAccount) {
        AccountData accountData = payloadAndAccount.getAccountData();
        AcmeJWSObject innerJws = payloadAndAccount.getPayload();

        boolean verified;
        //Check that the JWS protected header of the inner JWS has a “jwk” field.
        if (innerJws.getHeader().getJWK() == null) {
            return false;
        }

        //Check that the inner JWS verifies using the key in its “jwk” field.
        try {
            verified = SecurityValidatorUtil.verifyJWS(innerJws);
        } catch (AcmeServerException e) {
            log.error("Could not verify JWS", e);
            return false;
        }

        //Check that the payload of the inner JWS is a well-formed keyChange object (as described above).
        KeyChangeRequest keyChangeRequest;
        try {
            keyChangeRequest = SecurityValidatorUtil.getPayloadFromJWSObject(innerJws, KeyChangeRequest.class);
        } catch (AcmeServerException e) {
            log.error("Could not parse payload", e);
            return false;
        }

        //Check that the “url” parameters of the inner and outer JWSs are the same.
        if (!innerJws.getHeaderAcmeUrl().toString().contentEquals(innerJws.getHeader().getCustomParam("url").toString())) {
           return false;
        }

        //Check that the “account” field of the keyChange object contains the URL for the account matching the old key (i.e., the “kid” field in the outer JWS).
        if (StringUtils.isBlank(innerJws.getHeader().getKeyID()) ||
                !innerJws.getHeader().getKeyID().contentEquals(keyChangeRequest.getAccount())) {
            return false;
        }

        //Check that the “oldKey” field of the keyChange object is the same as the account key for the account in question.
        try {
            if (!JWK.parse(keyChangeRequest.getOldKey()).equals(JWK.parse(accountData.getJwk()))) {
                return false;
            }
        } catch (ParseException e) {
            log.error("Could not parse JWK", e);
            return false;
        }

        //Check that no account exists whose account key is the same as the key in the “jwk” header parameter of the inner JWS.
        Optional<AccountData> existing = accountPersistence.findFirstByJwkEquals(innerJws.getHeader().getJWK().toString());
        if (existing.isPresent()) return false;

        return verified;
    }

    //Verify account request meets requirements
    private void validateAccountRequest(AccountRequest accountRequest) throws AcmeServerException {
        ProblemDetails problemDetails = new ProblemDetails(ProblemType.COMPOUND);
        ProblemDetails contactError = validateContactField(accountRequest);
        if (contactError != null) {
            problemDetails.addSubproblem(contactError);

            throw new AcmeServerException(problemDetails);
        }
    }

    private ProblemDetails validateContactField(AccountRequest accountRequest) {
        if (accountRequest.getContact() != null && accountRequest.getContact().length > 0) {
            for (String contact : accountRequest.getContact()) {
                if (!validateEmail(contact)) {
                    ProblemDetails problemDetails = new ProblemDetails(ProblemType.INVALID_CONTACT);
                    problemDetails.setDetail("Contact not valid: "+contact);
                    return problemDetails;
                }
            }
        }
        return null;
    }

    private boolean validateEmail(String email) {
        String regex = "^([a-zA-Z0-9_\\-\\.]+)@([a-zA-Z0-9_\\-\\.]+)\\.([a-zA-Z]{2,5})$";
        return email.replace("mailto:","").matches(regex);
    }

    private boolean checkChangeInTermsOfService(AccountData accountData, DirectoryData directory) {
        if(directory.getObject().getMeta().getTermsOfService() != null){
            //has never agreed to terms
            if(accountData.getLastAgreedToTermsOfServiceOn() == null){
                return true;
            }
            //terms have been updated since last agreement
            return directory.getTermsOfServiceLastUpdatedOn().after(accountData.getLastAgreedToTermsOfServiceOn());
        }
        return false;
    }
}
