package com.winllc.acme.server.transaction;

import com.winllc.acme.common.contants.ProblemType;
import com.winllc.acme.common.contants.StatusType;
import com.winllc.acme.common.model.acme.Identifier;
import com.winllc.acme.common.model.acme.Order;
import com.winllc.acme.common.model.acme.ProblemDetails;
import com.winllc.acme.common.model.data.AuthorizationData;
import com.winllc.acme.common.model.data.OrderData;
import com.winllc.acme.common.model.requestresponse.OrderRequest;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.exceptions.AcmeServerException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 pending --------------+
    |                  |
    | All authz        |
    | "valid"          |
    V                  |
  ready ---------------+
    |                  |
    | Receive          |
    | finalize         |
    | request          |
    V                  |
processing ------------+
    |                  |
    | Certificate      | Error or
    | issued           | Authorization failure
    V                  V
  valid             invalid
 */
public class OrderDataWrapper extends DataWrapper<OrderData> {
    private static final Logger log = LogManager.getLogger(OrderDataWrapper.class);

    OrderData orderData;
    CertDataWrapper issuedCert;
    List<AuthorizationDataWrapper> authorizationDataWrappers;

    OrderDataWrapper(OrderRequest orderRequest, TransactionContext transactionContext) throws Exception {
        super(transactionContext);
        init(orderRequest);
    }

    OrderDataWrapper(OrderData orderData, TransactionContext transactionContext) throws Exception {
        super(transactionContext);

        Optional<OrderData> optionalData = transactionContext.getOrderPersistence().findById(orderData.getId());
        if(optionalData.isPresent()){
            this.orderData = orderData;

            reloadChildren();
        }else{
            throw new Exception("Could not find OrderData in DB: "+orderData.getId());
        }
    }


    void init(OrderRequest orderRequest) throws Exception {
        this.authorizationDataWrappers = new ArrayList<>();

        Order order = new Order();
        order.setStatus(StatusType.PENDING.toString());
        order.setIdentifiers(orderRequest.getIdentifiers());
        order.willExpireInMinutes(30);
        order.addNotBefore(LocalDateTime.now().minusDays(1));
        order.addNotAfter(LocalDateTime.now().plusDays(1));

        //get pre-authz, use this for any identifiers that match the request
        AccountDataWrapper accountDataWrapper = new AccountDataWrapper(transactionContext.getAccountData(), transactionContext);
        Map<Identifier, AuthorizationData> preAuthzIdentifierMap = accountDataWrapper.getPreAuthzList().stream()
                .collect(Collectors.toMap(ad -> ad.getObject().getIdentifier(), ad -> ad));

        this.orderData = new OrderData(order, transactionContext.getDirectoryData().getName(),
                transactionContext.getAccountData().getId());
        this.orderData.getObject().setFinalize(orderData.buildUrl(Application.baseURL)+"/finalize");

        if (orderRequest.getIdentifiers() != null) {
            ProblemDetails compoundProblems = new ProblemDetails(ProblemType.COMPOUND);
            for (Identifier identifier : orderRequest.getIdentifiers()) {
                IdentifierWrapper identifierWrapper = new IdentifierWrapper(identifier, transactionContext);
                if(identifierWrapper.canIssue()) {

                    AuthorizationData preAuthz = preAuthzIdentifierMap.get(identifier);
                    if (preAuthz == null) {
                        try {
                            AuthorizationDataWrapper authorizationDataWrapper =
                                    new AuthorizationDataWrapper(this, identifier, super.transactionContext);
                            this.authorizationDataWrappers.add(authorizationDataWrapper);
                        } catch (AcmeServerException e) {
                            log.error("COuld not create pre-authz", e);
                            compoundProblems.addSubproblem(e.getProblemDetails());
                        }
                    } else {
                        //Add pre-authz data to list
                        AuthorizationDataWrapper authorizationDataWrapper =
                                new AuthorizationDataWrapper(this, preAuthz, super.transactionContext);
                        this.authorizationDataWrappers.add(authorizationDataWrapper);
                    }
                }else{
                    //todo verify this is the right error type
                    ProblemDetails problemDetails = new ProblemDetails(ProblemType.REJECTED_IDENTIFIER);
                    problemDetails.setDetail("Will not issue to: "+identifier.getValue());
                    throw new AcmeServerException(problemDetails);
                }
            }

            if(compoundProblems.getSubproblems() != null && compoundProblems.getSubproblems().length > 0){
                throw new AcmeServerException(compoundProblems);
            }
        }else{
            throw new AcmeServerException(ProblemType.MALFORMED);
        }

        persist();
    }

    Set<Identifier> getIdentifiers(){
        return Stream.of(this.orderData.getObject().getIdentifiers()).collect(Collectors.toSet());
    }

    void markReady(){
        if(getStatus() == StatusType.PENDING) {
            updateStatus(StatusType.READY);
        }
    }

    void markProcessing(){
        if(getStatus() == StatusType.READY) {
            updateStatus(StatusType.PROCESSING);
        }
    }

    void markValid(){
        if(getStatus() == StatusType.PROCESSING) {
            updateStatus(StatusType.VALID);
        }
    }

    void markInvalid(){
        updateStatus(StatusType.INVALID);

        for(AuthorizationDataWrapper authz : authorizationDataWrappers){
            authz.markRevoked();
        }
    }

    void authzCompleted(){
        int authzValid = 0;
        int authzValidNeeded = authorizationDataWrappers.size();
        for(AuthorizationDataWrapper wrapper : authorizationDataWrappers){
            if(wrapper.getStatus() == StatusType.VALID){
                authzValid++;
            }
        }

        if(authzValid == authzValidNeeded){
            markReady();
        }
    }

    void finalizeOrder(String csr) {
        super.transactionContext.getTaskExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String eabKid = null;
                    if (StringUtils.isNotBlank(transactionContext.getAccountData().getEabKeyIdentifier())) {
                        eabKid = transactionContext.getAccountData().getEabKeyIdentifier();
                    }

                    X509Certificate certificate = transactionContext.getCa()
                            .issueCertificate(getIdentifiers(), eabKid, CertUtil.csrBase64ToPKC10Object(csr));
                    issuedCert = new CertDataWrapper(transactionContext);
                    issuedCert.certIssued(certificate);

                    orderData.getObject().setCertificate(issuedCert.certData.buildUrl(Application.baseURL));
                    markValid();
                    persist();
                } catch (Exception e) {
                    log.error("Could not finalize order", e);
                    markInvalid();
                }
            }
        });

    }

    @Override
    void reloadChildren() {
        authorizationDataWrappers = new ArrayList<>();

        List<AuthorizationData> authz = transactionContext.getAuthorizationPersistence().findAllByOrderIdEquals(this.orderData.getId());
        if(authz != null) {
            for (AuthorizationData authorizationData : authz) {
                AuthorizationDataWrapper wrapper = new AuthorizationDataWrapper(this, authorizationData, transactionContext);
                wrapper.reloadChildren();
                authorizationDataWrappers.add(wrapper);
            }
        }else{
            log.info("No authz found for order: "+orderData.getId());
        }
    }

    public OrderData getData(){
        if(this.orderData.getObject().isExpired()){
            markInvalid();
        }
        return this.orderData;
    }

    void persist() {
        this.orderData.addTransactionId(transactionContext.getTransactionId());
        this.orderData = transactionContext.getOrderPersistence().save(this.orderData);
    }
}