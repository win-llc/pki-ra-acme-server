package com.winllc.acme.server.process;

import com.nimbusds.jose.JWSObject;
import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.contants.StatusType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.exceptions.InternalServerException;
import com.winllc.acme.server.external.ExternalAccountProvider;
import com.winllc.acme.server.model.AcmeJWSObject;
import com.winllc.acme.server.model.acme.Account;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.acme.OrderList;
import com.winllc.acme.server.model.acme.ProblemDetails;
import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.DirectoryData;
import com.winllc.acme.server.model.data.OrderData;
import com.winllc.acme.server.model.data.OrderListData;
import com.winllc.acme.server.model.requestresponse.AccountRequest;
import com.winllc.acme.server.persistence.AccountPersistence;
import com.winllc.acme.server.persistence.OrderListPersistence;
import com.winllc.acme.server.persistence.OrderPersistence;
import com.winllc.acme.server.service.acme.AccountService;
import com.winllc.acme.server.service.internal.DirectoryDataService;
import com.winllc.acme.server.service.internal.ExternalAccountProviderService;
import com.winllc.acme.server.util.SecurityValidatorUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private DirectoryDataService directoryDataService;
    @Autowired
    private ExternalAccountProviderService externalAccountProviderService;


    public AccountData processCreateNewAccount(AcmeJWSObject jwsObject) throws AcmeServerException {
        AccountRequest accountRequest = SecurityValidatorUtil.getPayloadFromJWSObject(jwsObject, AccountRequest.class);
        DirectoryData directoryData = directoryDataService.findByName(jwsObject.getHeaderAcmeUrl().getDirectoryIdentifier());
        AccountData accountData = buildNew(directoryData);

        Directory directory = directoryData.getObject();
        //todo verify ToS workflow is to spec
        if (directory.getMeta() != null && directory.getMeta().getTermsOfService() != null) {
            if (!accountRequest.getTermsOfServiceAgreed()) {
                log.debug("Has not agreed to ToS");
                ProblemDetails problemDetails = new ProblemDetails(ProblemType.USER_ACTION_REQUIRED, 403);
                problemDetails.setDetail("Terms of Service agreement required");
                problemDetails.setInstance(directoryData.getObject().getMeta().getTermsOfService());

                log.error(problemDetails);

                throw new AcmeServerException(problemDetails);
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
                    ProblemDetails problemDetails = new ProblemDetails(ProblemType.EXTERNAL_ACCOUNT_REQUIRED, 403);

                    log.error(problemDetails);

                    throw new AcmeServerException(problemDetails);
                }

                ExternalAccountProvider accountProvider = externalAccountProviderService.findByName(directoryData.getExternalAccountProviderName());
                boolean verified = accountProvider.verifyExternalAccountJWS(jwsObject);

                if (verified) {
                    log.debug("External Account verified");
                    accountData.setJwk(jwsObject.getHeader().getJWK().toString());

                    JWSObject externalAccountJWS = null;
                    try {
                        externalAccountJWS = accountRequest.buildExternalAccountJWSObject();
                    } catch (ParseException e) {
                        ProblemDetails problemDetails = new ProblemDetails(ProblemType.SERVER_INTERNAL, 500);
                        problemDetails.setDetail(e.getMessage());

                        log.error(e);
                        throw new AcmeServerException(problemDetails);
                    }

                    if(externalAccountJWS != null) {
                        account.setExternalAccountBinding(accountRequest.getExternalAccountBinding().toJson());
                        accountData.setEabKeyIdentifier(externalAccountJWS.getHeader().getKeyID());
                    }else{
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

            account.setContact(accountRequest.getContact());

            accountData.setJwk(jwsObject.getHeader().getJWK().toString());

            accountData = accountPersistence.save(accountData);

            log.info("Account created: "+ accountData);

            return accountData;

        }else{
            log.error(validationError);

            throw new AcmeServerException(validationError);
        }
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


    public AccountData buildNew(DirectoryData directoryData){
        Account account = new Account();
        account.setStatus(StatusType.VALID.toString());

        //Set order list location, Section 7.1.2.1
        OrderList orderList = new OrderList();

        OrderListData orderListData = new OrderListData(orderList, directoryData.getName());
        orderListData = orderListPersistence.save(orderListData);

        account.setOrders(orderListData.buildUrl());

        AccountData accountData = new AccountData(account, directoryData.getName());

        return accountData;
    }

    //Section 7.3.6
    public AccountData deactivateAccount(AccountData accountData) throws InternalServerException {
        //if all goes well
        Account account = accountData.getObject();
        if(account.getStatus().contentEquals(StatusType.VALID.toString())) {
            accountData.getObject().setStatus(StatusType.DEACTIVATED.toString());
            accountData = accountPersistence.save(accountData);

            markInProgressAccountObjectsInvalid(accountData);

            log.info("Account deactivated: "+accountData.getId());

            return accountData;
        }else{
            throw new InternalServerException("Account was not in state to be set deactivated");
        }
    }

    public AccountData accountRevoke(AccountData accountData) throws InternalServerException {
        //if all goes well
        Account account = accountData.getObject();
        if(account.getStatus().contentEquals(StatusType.VALID.toString())) {
            account.setStatus(StatusType.REVOKED.toString());
            accountData = accountPersistence.save(accountData);

            markInProgressAccountObjectsInvalid(accountData);

            log.info("Account revoked: "+accountData.getId());

            return accountData;
        }else{
            throw new InternalServerException("Account was not in state to be set revoked");
        }
    }

    //The server SHOULD cancel any pending operations authorized by the account’s key, such as certificate orders
    private void markInProgressAccountObjectsInvalid(AccountData accountData){
        List<OrderData> orderDataList = orderPersistence.findAllByAccountIdEquals(accountData.getId());
        orderDataList.forEach(o -> {
            if(!o.getObject().getStatus().contentEquals(StatusType.VALID.toString())) {
                o.getObject().setStatus(StatusType.INVALID.toString());
                orderPersistence.save(o);
            }
        });
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
        //String regex = "^[\\w-_.+]*[\\w-_.]@([\\w]+\\.)+[\\w]+[\\w]$";
        //return email.replace("mailto:","").matches(regex);
        //todo add back
        return true;
    }
}
