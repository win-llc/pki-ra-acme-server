package com.winllc.acme.server.service.acme;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.jwk.JWK;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.external.ExternalAccount;
import com.winllc.acme.server.external.ExternalAccountProvider;
import com.winllc.acme.server.model.AcmeJWSObject;
import com.winllc.acme.server.model.acme.Account;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.acme.ProblemDetails;
import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.model.requestresponse.AccountRequest;
import com.winllc.acme.server.model.requestresponse.KeyChangeRequest;
import com.winllc.acme.server.persistence.AccountPersistence;
import com.winllc.acme.server.process.AccountProcessor;
import com.winllc.acme.server.util.AppUtil;
import com.winllc.acme.server.util.PayloadAndAccount;
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
import java.util.stream.Collectors;

//Section 7.3
@RestController
public class AccountService extends BaseService {

    private AccountPersistence accountPersistence = new AccountPersistence();
    private AccountProcessor accountProcessor;

    //Section 7.3
    @RequestMapping(value = "new-account", method = RequestMethod.POST, consumes = "application/jose+json")
    public ResponseEntity<?> request(HttpServletRequest request) throws Exception {

        HttpHeaders headers = new HttpHeaders();

        String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        AcmeJWSObject jwsObject = AcmeJWSObject.parse(body);

        if (AppUtil.verifyJWS(jwsObject)) {
            //TODO only proceed if true
        } else {
            //Invalid JWS
        }

        String jwk = jwsObject.getHeader().getJWK().toJSONString();

        AccountRequest accountRequest = buildAccountRequestFromHttpRequest(request);

        //Section 7.3.1
        Optional<AccountData> accountDataOptional = accountPersistence.getByJwk(jwk);
        //If account already present, don't recreate
        if (accountDataOptional.isPresent()) {
            AccountData accountData = accountDataOptional.get();
            headers.add("Location", accountData.buildUrl());

            return buildBaseResponseEntity(200)
                    .headers(headers).build();
        } else {
            //If no account exists, but account lookup requested, send error
            if (accountRequest.getOnlyReturnExisting()) {
                ProblemDetails problemDetails = new ProblemDetails(ProblemType.ACCOUNT_DOES_NOT_EXIST);
                return buildBaseResponseEntity(400)
                        .body(problemDetails);
            }
        }

        //Section 7.3.1 Paragraph 2
        //Not an account lookup at this point, create a new account
        if (!accountRequest.getOnlyReturnExisting()) {

            DirectoryData directory = Application.directoryDataMap.get(jwsObject.getHeaderAcmeUrl().getDirectoryIdentifier());

            //If terms of service exist, ensure client has agreed
            if (directory.getObject().getMeta().getTermsOfService() != null) {
                //TODO
                if (!accountRequest.getTermsOfServiceAgreed()) {
                    ProblemDetails problemDetails = new ProblemDetails(ProblemType.USER_ACTION_REQUIRED);
                    problemDetails.setDetail("Terms of Service agreement required");
                    //TODO provide terms location

                    return buildBaseResponseEntity(403)
                            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                            .body(problemDetails);
                }
            }

            //Validate the data in the account request for valid syntax and format
            if (validateAccountRequest(accountRequest) == null) {

                AccountData accountData = new AccountProcessor().buildNew(directory);
                Account account = accountData.getObject();

                //If external account required, perform further validation
                if (directory.getObject().getMeta().isExternalAccountRequired()) {
                    //TODO real logic
                    ExternalAccountProvider accountProvider = Application.accountProviders.get(0);
                    boolean verified = accountProvider.verifyExternalAccountJWS(accountRequest.getExternalAccountBinding());

                    if (verified) {
                        ExternalAccount externalAccount = new ExternalAccount();
                        //TODO
                        externalAccount.setAccountKey("");
                        account.setExternalAccountBinding(externalAccount);
                    } else {
                        //reject and return
                    }
                }

                account.setContact(accountRequest.getContact());

                accountData.setJwk(jwsObject.getHeader().getJWK().toString());

                accountPersistence.save(accountData);

                headers.add("Replay-Nonce", AppUtil.generateNonce());
                headers.add("Link", "TODO");
                headers.add("Location", accountData.buildUrl());

                return ResponseEntity.status(201)
                        .headers(headers)
                        .body(account);

            }

        }
        return null;
    }

    //Section 7.3.2
    @RequestMapping(value = "acct/{id}", method = RequestMethod.POST, consumes = "application/jose+json")
    public ResponseEntity<?> update(@PathVariable String id, HttpServletRequest request) throws Exception {
        PayloadAndAccount<AccountRequest> payloadAndAccount = AppUtil.verifyJWSAndReturnPayloadForExistingAccount(request, id, AccountRequest.class);

        AccountRequest accountRequest = payloadAndAccount.getPayload();
        Optional<AccountData> optionalAccountData = accountPersistence.getByAccountId(id);


        if (optionalAccountData.isPresent()) {
            AccountData accountData = optionalAccountData.get();

            //Section 7.3.6
            if (accountRequest.getStatus().contentEquals(StatusType.DEACTIVATED.toString())) {

                accountData = accountProcessor.deactivateAccount(accountData);

                return buildBaseResponseEntity(200)
                        .body(accountData.getObject());
            }

            //Section 7.3.3
            if (!checkChangeInTermsOfService(accountData, Application.directory)) {

                if (validateContactField(accountRequest) == null) {
                    accountData.getObject().setContact(accountRequest.getContact());

                    accountData = accountPersistence.save(accountData);

                    return buildBaseResponseEntity(200)
                            .body(accountData);
                }
            } else {
                //TODO return a 403

                ProblemDetails problemDetails = new ProblemDetails(ProblemType.USER_ACTION_REQUIRED);
                problemDetails.setDetail("Terms of service have changed");
                //TODO
                problemDetails.setInstance("TODO");
                return buildBaseResponseEntity(403)
                        //TODO headers
                        .body(problemDetails);
            }
        }
        return null;
    }

    //Section 7.3.5
    @RequestMapping(value = "key-change", method = RequestMethod.POST, consumes = "application/jose+json")
    public ResponseEntity<?> keyRollover(HttpServletRequest httpRequest) throws Exception {

        PayloadAndAccount<AcmeJWSObject> payloadAndAccount = AppUtil.verifyJWSAndReturnPayloadForExistingAccount(httpRequest, AcmeJWSObject.class);
        AccountData accountData = payloadAndAccount.getAccountData();

        if (validateJWSForKeyChange(payloadAndAccount, payloadAndAccount.getPayload())) {
            //TODO
            String newKey = payloadAndAccount.getPayload().getHeader().getJWK().toString();
            accountData.setJwk(newKey);

            accountPersistence.save(accountData);

            return buildBaseResponseEntity(200)
                    .body(accountData.getObject());
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", payloadAndAccount.getAccountData().buildUrl());

            //TODO
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.UNAUTHORIZED);

            //TODO, only for conflict
            return buildBaseResponseEntity(409)
                    .headers(headers)
                    .body(problemDetails);
        }
    }

    private AccountRequest buildAccountRequestFromHttpRequest(HttpServletRequest request) throws Exception {
        String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        JWSObject jwsObject = JWSObject.parse(body);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jwsObject.getPayload().toJSONObject().toJSONString(), AccountRequest.class);
    }

    //Section 7.3.5
    //Must verify the inner JWS Object before updating account key
    private boolean validateJWSForKeyChange(PayloadAndAccount<AcmeJWSObject> payloadAndAccount, AcmeJWSObject innerJws) {
        AccountData accountData = payloadAndAccount.getAccountData();
        AcmeJWSObject outerJws = payloadAndAccount.getPayload();

        boolean valid = true;
        KeyChangeRequest keyChangeRequest = null;
        //Check that the JWS protected header of the inner JWS has a “jwk” field.
        if (innerJws.getHeader().getJWK() == null) {
            valid = false;
        }

        //Check that the inner JWS verifies using the key in its “jwk” field.
        try {
            valid = AppUtil.verifyJWS(innerJws);
        } catch (AcmeServerException e) {
            e.printStackTrace();
            valid = false;
        }

        //Check that the payload of the inner JWS is a well-formed keyChange object (as described above).
        try {
            keyChangeRequest = AppUtil.getPayloadFromJWSObject(innerJws, KeyChangeRequest.class);
        } catch (AcmeServerException e) {
            e.printStackTrace();
            return false;
        }

        //Check that the “url” parameters of the inner and outer JWSs are the same.
        if (!outerJws.getHeaderAcmeUrl().toString().contentEquals(innerJws.getHeaderAcmeUrl().toString())) {
            valid = false;
        }

        //Check that the “account” field of the keyChange object contains the URL for the account matching the old key (i.e., the “kid” field in the outer JWS).
        if (!outerJws.getHeader().getKeyID()
                .contentEquals(keyChangeRequest.getAccount())) {
            valid = false;
        }

        //Check that the “oldKey” field of the keyChange object is the same as the account key for the account in question.
        try {
            if (!keyChangeRequest.getOldKey().equals(JWK.parse(accountData.getJwk()))) {
                valid = false;
            }
        } catch (ParseException e) {
            e.printStackTrace();
            valid = false;
        }

        //Check that no account exists whose account key is the same as the key in the “jwk” header parameter of the inner JWS.
        Optional<AccountData> existing = accountPersistence.getByJwk(innerJws.getHeader().getJWK().toString());
        if (existing.isPresent()) valid = false;

        return valid;
    }

    private ProblemDetails validateAccountRequest(AccountRequest accountRequest) {
        //TODO
        if (validateContactField(accountRequest) != null) {

        }

        return null;
    }

    private ProblemDetails validateContactField(AccountRequest accountRequest) {
        if (accountRequest.getContact() != null && accountRequest.getContact().length > 0) {
            for (String contact : accountRequest.getContact()) {
                if (!validateEmail(contact)) {
                    ProblemDetails problemDetails = new ProblemDetails(ProblemType.INVALID_CONTACT);
                    problemDetails.setDetail("Contact not valid");
                }
            }
        }
        return null;
    }

    private boolean validateEmail(String email) {
        //TODO
        return false;
    }

    private boolean checkChangeInTermsOfService(AccountData accountData, Directory directory) {
        //TODO
        return true;
    }
}
