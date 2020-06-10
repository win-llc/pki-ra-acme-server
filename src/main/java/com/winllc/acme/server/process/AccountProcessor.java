package com.winllc.acme.server.process;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.winllc.acme.common.contants.ProblemType;
import com.winllc.acme.common.contants.StatusType;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.exceptions.InternalServerException;
import com.winllc.acme.server.external.ExternalAccountProvider;
import com.winllc.acme.common.model.AcmeJWSObject;
import com.winllc.acme.common.model.acme.Account;
import com.winllc.acme.common.model.acme.Directory;
import com.winllc.acme.common.model.acme.OrderList;
import com.winllc.acme.common.model.acme.ProblemDetails;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.common.model.data.OrderData;
import com.winllc.acme.common.model.data.OrderListData;
import com.winllc.acme.common.model.requestresponse.AccountRequest;
import com.winllc.acme.server.persistence.AccountPersistence;
import com.winllc.acme.server.persistence.OrderListPersistence;
import com.winllc.acme.server.persistence.OrderPersistence;
import com.winllc.acme.server.service.internal.ExternalAccountProviderService;
import com.winllc.acme.server.util.SecurityValidatorUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/*
                  valid
                    |
                    |
        +-----------+-----------+
 Client |                Server |
deactiv.|                revoke |
        V                       V
   deactivated               revoked
 */
@Component
public class AccountProcessor implements AcmeDataProcessor<AccountData> {
    private static final Logger log = LogManager.getLogger(AccountProcessor.class);

    @Autowired
    private AccountPersistence accountPersistence;
    @Autowired
    private OrderPersistence orderPersistence;
    @Autowired
    private OrderListPersistence orderListPersistence;
    @Autowired
    private ExternalAccountProviderService externalAccountProviderService;


    public AccountData processCreateNewAccount(AccountRequest accountRequest, DirectoryData directoryData,
                                               AcmeJWSObject jwsObject) throws AcmeServerException {
        AccountData accountData = buildNew(directoryData);
        Account account = accountData.getObject();
        Directory directory = directoryData.getObject();

        //todo verify ToS workflow is to spec
        if (directory.termsOfServiceRequired()) {
            if (accountRequest.getTermsOfServiceAgreed()) {
                accountData.setLastAgreedToTermsOfServiceOn(Timestamp.valueOf(LocalDateTime.now()));
            } else {
                //If ToS agreed to, update last agreed to date
                log.debug("Has not agreed to ToS");
                ProblemDetails problemDetails = new ProblemDetails(ProblemType.USER_ACTION_REQUIRED, 403);
                problemDetails.setDetail("Terms of Service agreement required");
                problemDetails.setInstance(directoryData.getObject().getMeta().getTermsOfService());

                log.error(problemDetails);

                throw new AcmeServerException(problemDetails);
            }
        }

        //Validate the data in the account request for valid syntax and format
        validateAccountRequest(accountRequest);

        account.setContact(accountRequest.getContact());

        //If external account required, perform further validation
        if (directory.getMeta().isExternalAccountRequired()) {
            processExternalAccountRequiredRequest(accountData, accountRequest, jwsObject, directoryData);
        }

        account.setContact(accountRequest.getContact());

        accountData.setJwk(jwsObject.getHeader().getJWK().toString());

        accountData = accountPersistence.save(accountData);

        log.info("Account created: " + accountData);

        return accountData;
    }

    public AccountData processReturnExisting(AcmeJWSObject jwsObject) throws AcmeServerException {
        String jwk = jwsObject.getHeader().getJWK().toJSONString();
        //Section 7.3.1
        Optional<AccountData> accountDataOptional = accountPersistence.findFirstByJwkEquals(jwk);
        //If account already present, don't recreate
        if (accountDataOptional.isPresent()) {
            AccountData accountData = accountDataOptional.get();

            return accountData;
        } else {
            //If no account exists, but account lookup requested, send error
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.ACCOUNT_DOES_NOT_EXIST, 400);
            throw new AcmeServerException(problemDetails);
        }
    }

    private void processExternalAccountRequiredRequest(AccountData accountData, AccountRequest accountRequest,
                                                       AcmeJWSObject accountRequestJws,
                                                       DirectoryData directoryData) throws AcmeServerException {
        Account account = accountData.getObject();
        //If external account required, but none provided, return error
        if (accountRequest.getExternalAccountBinding() == null) {
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.EXTERNAL_ACCOUNT_REQUIRED, 403);

            log.error(problemDetails);

            throw new AcmeServerException(problemDetails);
        }

        JWSObject innerJwsObject = retrieveExternalAccountJWS(accountRequestJws);

        ExternalAccountProvider accountProvider = externalAccountProviderService.findByName(directoryData.getExternalAccountProviderName());
        boolean verified = accountProvider.verifyAccountBinding(innerJwsObject, accountRequestJws);

        if (verified) {
            log.debug("External Account verified");
            accountData.setJwk(accountRequestJws.getHeader().getJWK().toString());

            JWSObject externalAccountJWS;
            try {
                externalAccountJWS = accountRequest.buildExternalAccountJWSObject();
            } catch (ParseException e) {
                ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL, 500);
                problemDetails.setDetail(e.getMessage());

                log.error("Could not parse externalAccount JWS", e);
                throw new AcmeServerException(problemDetails);
            }

            if (externalAccountJWS != null) {
                account.setExternalAccountBinding(accountRequest.getExternalAccountBinding().toJson());
                accountData.setEabKeyIdentifier(externalAccountJWS.getHeader().getKeyID());
            } else {
                ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL, 500);
                problemDetails.setDetail("Could not build JWS External Account Object");
                throw new AcmeServerException(problemDetails);
            }
        } else {
            //reject and return
            ProblemDetails problemDetails = new ProblemDetails(ProblemType.MALFORMED, 403);
            problemDetails.setDetail("Could not verify external account");

            log.error(problemDetails);

            throw new AcmeServerException(problemDetails);
        }
    }

        /*
        The ACME client then computes a binding JWS to indicate the external account holder’s approval of the ACME account key.
        The payload of this JWS is the ACME account key being registered, in JWK form. The protected header of the JWS MUST meet the following criteria:

    The “alg” field MUST indicate a MAC-based algorithm
    The “kid” field MUST contain the key identifier provided by the CA
    The “nonce” field MUST NOT be present
    The “url” field MUST be set to the same value as the outer JWS
         */

    private JWSObject retrieveExternalAccountJWS(AcmeJWSObject outerObject) throws AcmeServerException {
        AccountRequest innerObjectString = SecurityValidatorUtil.getPayloadFromJWSObject(outerObject, AccountRequest.class);
        JWSObject innerObject;
        try {
            innerObject = innerObjectString.buildExternalAccountJWSObject();
        } catch (ParseException e) {
            throw new AcmeServerException(ProblemType.SERVER_INTERNAL);
        }

        JWSHeader header = innerObject.getHeader();

        //The “alg” field MUST indicate a MAC-based algorithm
        if (!JWSAlgorithm.Family.HMAC_SHA.contains(header.getAlgorithm())) {
            log.error("External Account Validation: Invalid algorithm");
            throw new AcmeServerException(ProblemType.SERVER_INTERNAL);
        }

        //The “nonce” field MUST NOT be present
        if (header.getCustomParam("nonce") != null) {
            log.error("External Account Validation: Cannot contain nonce");
            throw new AcmeServerException(ProblemType.SERVER_INTERNAL);
        }

        //The “url” field MUST be set to the same value as the outer JWS
        String innerUrl = innerObject.getHeader().getCustomParam("url").toString();
        String outerUrl = outerObject.getHeaderAcmeUrl().toString();
        if (!innerUrl.equalsIgnoreCase(outerUrl)) {
            log.error("External Account Validation: URL of innner and outer JWS did not match");
            throw new AcmeServerException(ProblemType.SERVER_INTERNAL);
        }

        //The “kid” field MUST contain the key identifier provided by the CA
        return innerObject;
    }


    public AccountData buildNew(DirectoryData directoryData) {
        if (directoryData == null) throw new IllegalArgumentException("DirectoryData can't be null");

        Account account = new Account();
        account.markValid();

        //Set order list location, Section 7.1.2.1
        OrderList orderList = new OrderList();

        OrderListData orderListData = new OrderListData(orderList, directoryData.getName());
        orderListData = orderListPersistence.save(orderListData);

        account.setOrders(orderListData.buildUrl(Application.baseURL));

        AccountData accountData = new AccountData(account, directoryData.getName());

        return accountData;
    }

    //Section 7.3.6
    public AccountData deactivateAccount(AccountData accountData) throws InternalServerException {
        //if all goes well
        Account account = accountData.getObject();
        if (account.checkStatusEquals(StatusType.VALID)) {
            accountData.getObject().markDeactivated();
            accountData = accountPersistence.save(accountData);

            markInProgressAccountObjectsInvalid(accountData);

            log.info("Account deactivated: " + accountData.getId());

            return accountData;
        } else {
            throw new InternalServerException("Account was not in state to be set deactivated");
        }
    }

    public AccountData accountRevoke(AccountData accountData) throws InternalServerException {
        //if all goes well
        Account account = accountData.getObject();
        if (account.checkStatusEquals(StatusType.VALID)) {
            account.markRevoked();
            accountData = accountPersistence.save(accountData);

            markInProgressAccountObjectsInvalid(accountData);

            log.info("Account revoked: " + accountData.getId());

            return accountData;
        } else {
            throw new InternalServerException("Account was not in state to be set revoked");
        }
    }

    //The server SHOULD cancel any pending operations authorized by the account’s key, such as certificate orders
    //todo move this to order processor
    private void markInProgressAccountObjectsInvalid(AccountData accountData) {
        List<OrderData> orderDataList = orderPersistence.findAllByAccountIdEquals(accountData.getId());
        orderDataList.forEach(o -> {
            if (!o.getObject().checkStatusEquals(StatusType.VALID)) {
                o.getObject().setStatus(StatusType.INVALID.toString());
                orderPersistence.save(o);
            }
        });
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
                    problemDetails.setDetail("Contact not valid: " + contact);
                    return problemDetails;
                }
            }
        }
        return null;
    }

    private boolean validateEmail(String email) {
        //String regex = "^[\\w-_.+]*[\\w-_.]@([\\w]+\\.)+[\\w]+[\\w]$";
        //return email.replace("mailto:","").matches(regex);
        //todo add back
        return true;
    }
}
