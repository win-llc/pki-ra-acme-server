package com.winllc.acme.server.service.acme;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.jwk.JWK;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.external.ExternalAccountProvider;
import com.winllc.acme.server.model.AcmeJWSObject;
import com.winllc.acme.server.model.AcmeURL;
import com.winllc.acme.server.model.acme.Account;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.acme.ProblemDetails;
import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.model.requestresponse.AccountRequest;
import com.winllc.acme.server.model.requestresponse.KeyChangeRequest;
import com.winllc.acme.server.persistence.AccountPersistence;
import com.winllc.acme.server.process.AccountProcessor;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import com.winllc.acme.server.service.internal.ExternalAccountProviderService;
import com.winllc.acme.server.util.SecurityValidatorUtil;
import com.winllc.acme.server.util.PayloadAndAccount;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Collectors;

//Section 7.3
@RestController
public class AccountService extends BaseService {

    private static final Logger log = LogManager.getLogger(AccountService.class);

    @Autowired
    private AccountPersistence accountPersistence;
    @Autowired
    private AccountProcessor accountProcessor;
    @Autowired
    private DirectoryDataService directoryDataService;
    @Autowired
    private SecurityValidatorUtil securityValidatorUtil;
    @Autowired
    private ExternalAccountProviderService externalAccountProviderService;

    //Section 7.3
    @RequestMapping(value = "{directory}/new-account", method = RequestMethod.POST, consumes = "application/jose+json")
    public ResponseEntity<?> request(HttpServletRequest request, @PathVariable String directory) throws Exception {
        log.debug("new-account request");
        DirectoryData directoryData = directoryDataService.getByName(new AcmeURL(request).getDirectoryIdentifier());

        String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

        AcmeJWSObject jwsObject = AcmeJWSObject.parse(body);

        //If JWS invalid, don't proceed
        if (!SecurityValidatorUtil.verifyJWS(jwsObject)) {
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.MALFORMED);

            log.error(problemDetails);

            return buildBaseResponseEntity(500, directoryData)
                    .body(problemDetails);
        }

        String jwk = jwsObject.getHeader().getJWK().toJSONString();
        AccountRequest accountRequest = SecurityValidatorUtil.getPayloadFromJWSObject(jwsObject, AccountRequest.class);

        //Section 7.3.1 Paragraph 2
        if (!accountRequest.getOnlyReturnExisting()) {
            //If terms of service exist, ensure client has agreed
            return processCreateNewAccount(accountRequest, directoryData, jwsObject);
        }else{
            return processReturnExisting(directoryData, jwk);
        }
    }

    private ResponseEntity<?> processCreateNewAccount(AccountRequest accountRequest, DirectoryData directoryData, AcmeJWSObject jwsObject) throws Exception{
        AccountData accountData = accountProcessor.buildNew(directoryData);

        if (directoryData.getObject().getMeta().getTermsOfService() != null) {
            if (!accountRequest.getTermsOfServiceAgreed()) {
                log.debug("Has not agreed to ToS");
                ProblemDetails problemDetails = new ProblemDetails(ProblemType.USER_ACTION_REQUIRED);
                problemDetails.setDetail("Terms of Service agreement required");
                problemDetails.setInstance(directoryData.getObject().getMeta().getTermsOfService());

                log.error(problemDetails);

                return buildBaseResponseEntity(403, directoryData)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body(problemDetails);
            }else{
                //If ToS agreed to, update last agreed to date
                accountData.setLastAgreedToTermsOfServiceOn(Timestamp.valueOf(LocalDateTime.now()));
            }
        }

        //Validate the data in the account request for valid syntax and format
        ProblemDetails validationError = validateAccountRequest(accountRequest);
        if (validationError == null) {


            Account account = accountData.getObject();

            //If external account required, perform further validation
            if (directoryData.getObject().getMeta().isExternalAccountRequired()) {
                //If external account required, but none provided, return error
                if(accountRequest.getExternalAccountBinding() == null){
                    ProblemDetails problemDetails = new ProblemDetails(ProblemType.EXTERNAL_ACCOUNT_REQUIRED);

                    log.error(problemDetails);

                    return buildBaseResponseEntity(403, directoryData)
                            .body(problemDetails);
                }

                //TODO
                ExternalAccountProvider accountProvider = externalAccountProviderService.findByName(directoryData.getExternalAccountProviderName());
                boolean verified = accountProvider.verifyExternalAccountJWS(jwsObject);

                if (verified) {
                    log.debug("External Account verified");
                    accountData.setJwk(jwsObject.getHeader().getJWK().toString());

                    JWSObject externalAccountJWS = accountRequest.buildExternalAccountJWSObject();

                    if(externalAccountJWS != null) {
                        account.setExternalAccountBinding(accountRequest.getExternalAccountBinding().toJson());
                    }else{
                        throw new Exception("Could not build JWS External Account Object");
                    }
                } else {
                    //reject and return
                    ProblemDetails problemDetails = new ProblemDetails(ProblemType.MALFORMED);
                    problemDetails.setDetail("Could not verify external account");

                    log.error(problemDetails);

                    return buildBaseResponseEntity(403, directoryData)
                            .body(problemDetails);
                }
            }

            account.setContact(accountRequest.getContact());

            accountData.setJwk(jwsObject.getHeader().getJWK().toString());

            accountData = accountPersistence.save(accountData);

            log.info("Account created: "+ accountData);

            return buildBaseResponseEntity(201, directoryData)
                    .header("Location", accountData.buildUrl())
                    .body(accountData.getObject());

        }else{
            log.error(validationError);
            return buildBaseResponseEntity(500, directoryData)
                    .body(validationError);
        }
    }

    private ResponseEntity<?> processReturnExisting(DirectoryData directoryData, String jwk){
        //Section 7.3.1
        Optional<AccountData> accountDataOptional = accountPersistence.getByJwk(jwk);
        //If account already present, don't recreate
        if (accountDataOptional.isPresent()) {
            AccountData accountData = accountDataOptional.get();

            return buildBaseResponseEntity(200, directoryData)
                    .header("Location", accountData.buildUrl())
                    .body(accountData.getObject());
        } else {
            //If no account exists, but account lookup requested, send error
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.ACCOUNT_DOES_NOT_EXIST);
            return buildBaseResponseEntity(400, directoryData)
                    .body(problemDetails);
        }
    }

    //Section 7.3.2
    @RequestMapping(value = "{directory}/acct/{id}", method = RequestMethod.POST, consumes = "application/jose+json")
    public ResponseEntity<?> update(@PathVariable String id, HttpServletRequest request, @PathVariable String directory) throws Exception {
        PayloadAndAccount<AccountRequest> payloadAndAccount = securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(request, id, AccountRequest.class);

        AccountRequest accountRequest = payloadAndAccount.getPayload();
        DirectoryData directoryData = payloadAndAccount.getDirectoryData();
        Optional<AccountData> optionalAccountData = accountPersistence.getByAccountId(id);

        if (optionalAccountData.isPresent()) {
            AccountData accountData = optionalAccountData.get();

            if(!accountData.getObject().getStatus().equalsIgnoreCase(StatusType.DEACTIVATED.toString())) {
                //Section 7.3.6
                if (StringUtils.isNotBlank(accountRequest.getStatus()) && accountRequest.getStatus().contentEquals(StatusType.DEACTIVATED.toString())) {

                    accountData = accountProcessor.deactivateAccount(accountData);

                    return buildBaseResponseEntity(200, payloadAndAccount.getDirectoryData())
                            .body(accountData.getObject());
                }

                //Section 7.3.3
                if (!checkChangeInTermsOfService(accountData, directoryData)) {
                    ProblemDetails problemDetails = validateContactField(accountRequest);
                    if (problemDetails == null) {
                        accountData.getObject().setContact(accountRequest.getContact());

                        accountData = accountPersistence.save(accountData);

                        return buildBaseResponseEntity(200, payloadAndAccount.getDirectoryData())
                                .body(accountData);
                    }else{
                        return buildBaseResponseEntity(500, directoryData)
                                .body(problemDetails);
                    }
                } else {
                    ProblemDetails problemDetails = new ProblemDetails(ProblemType.USER_ACTION_REQUIRED);
                    problemDetails.setDetail("Terms of service have changed");
                    //TODO
                    problemDetails.setInstance("TODO");
                    return buildBaseResponseEntity(403, payloadAndAccount.getDirectoryData())
                            //TODO headers
                            .body(problemDetails);
                }
            }else{
                ProblemDetails problemDetails = new ProblemDetails(ProblemType.UNAUTHORIZED);
                problemDetails.setDetail("Account deactivated, can't update");

                log.error(problemDetails);

                return buildBaseResponseEntity(403, payloadAndAccount.getDirectoryData())
                        .body(problemDetails);

            }
        }else {
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL);
            problemDetails.setDetail("Could not find account");

            log.error(problemDetails);

            return buildBaseResponseEntity(500, payloadAndAccount.getDirectoryData())
                    .body(problemDetails);
        }
    }

    //Section 7.3.5
    @RequestMapping(value = "{directory}/key-change", method = RequestMethod.POST, consumes = "application/jose+json")
    public ResponseEntity<?> keyRollover(HttpServletRequest httpRequest, @PathVariable String directory) throws Exception {

        PayloadAndAccount<AcmeJWSObject> payloadAndAccount = securityValidatorUtil.verifyJWSAndReturnPayloadForExistingAccount(httpRequest, AcmeJWSObject.class);
        AccountData accountData = payloadAndAccount.getAccountData();

        if (validateJWSForKeyChange(payloadAndAccount, payloadAndAccount.getPayload())) {
            //TODO
            String newKey = payloadAndAccount.getPayload().getHeader().getJWK().toString();
            accountData.setJwk(newKey);

            accountPersistence.save(accountData);

            log.info("key-change success for "+accountData);

            return buildBaseResponseEntity(200, payloadAndAccount.getDirectoryData())
                    .body(accountData.getObject());
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", payloadAndAccount.getAccountData().buildUrl());

            //TODO
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.UNAUTHORIZED);

            log.error(problemDetails);

            //TODO, only for conflict
            return buildBaseResponseEntity(409, payloadAndAccount.getDirectoryData())
                    .headers(headers)
                    .body(problemDetails);
        }
    }

    //Section 7.3.5
    //Must verify the inner JWS Object before updating account key
    private boolean validateJWSForKeyChange(PayloadAndAccount<AcmeJWSObject> payloadAndAccount, AcmeJWSObject innerJws) {
        AccountData accountData = payloadAndAccount.getAccountData();
        AcmeJWSObject outerJws = payloadAndAccount.getPayload();

        boolean verified = false;
        KeyChangeRequest keyChangeRequest = null;
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
        try {
            keyChangeRequest = SecurityValidatorUtil.getPayloadFromJWSObject(innerJws, KeyChangeRequest.class);
        } catch (AcmeServerException e) {
            log.error("Could not parse payload", e);
            return false;
        }

        //Check that the “url” parameters of the inner and outer JWSs are the same.
        if (!outerJws.getHeaderAcmeUrl().toString().contentEquals(innerJws.getHeaderAcmeUrl().toString())) {
           return false;
        }

        //Check that the “account” field of the keyChange object contains the URL for the account matching the old key (i.e., the “kid” field in the outer JWS).
        if (!outerJws.getHeader().getKeyID()
                .contentEquals(keyChangeRequest.getAccount())) {
            return false;
        }

        //Check that the “oldKey” field of the keyChange object is the same as the account key for the account in question.
        try {
            if (!keyChangeRequest.getOldKey().equals(JWK.parse(accountData.getJwk()))) {
                return false;
            }
        } catch (ParseException e) {
            log.error("Could not parse JWK", e);
            return false;
        }

        //Check that no account exists whose account key is the same as the key in the “jwk” header parameter of the inner JWS.
        Optional<AccountData> existing = accountPersistence.getByJwk(innerJws.getHeader().getJWK().toString());
        if (existing.isPresent()) return false;

        return verified;
    }

    //Verify account request meets requirements
    private ProblemDetails validateAccountRequest(AccountRequest accountRequest) {
        ProblemDetails problemDetails = new ProblemDetails(ProblemType.COMPOUND);
        ProblemDetails contactError = validateContactField(accountRequest);
        if (contactError != null) {
            problemDetails.addSubproblem(contactError);
            return problemDetails;
        }

        return null;
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
        String regex = "^[\\w-_.+]*[\\w-_.]@([\\w]+\\.)+[\\w]+[\\w]$";
        return email.replace("mailto:","").matches(regex);
    }

    private boolean checkChangeInTermsOfService(AccountData accountData, DirectoryData directory) {
        if(directory.getObject().getMeta().getTermsOfService() != null){
            //has never agreed to terms
            if(accountData.getLastAgreedToTermsOfServiceOn() == null){
                return true;
            }
            //terms have been updated since last agreement
            if(directory.getTermsOfServiceLastUpdatedOn().after(accountData.getLastAgreedToTermsOfServiceOn())){
                return true;
            }
        }
        return false;
    }
}
