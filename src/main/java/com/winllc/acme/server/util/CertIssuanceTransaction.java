package com.winllc.acme.server.util;

import com.winllc.acme.common.contants.ChallengeType;
import com.winllc.acme.common.contants.StatusType;
import com.winllc.acme.common.model.acme.*;
import com.winllc.acme.common.model.data.*;
import com.winllc.acme.common.model.requestresponse.OrderRequest;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.acme.server.Application;
import com.winllc.acme.server.challenge.HttpChallengeRunner;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.acme.server.persistence.AuthorizationPersistence;
import com.winllc.acme.server.persistence.CertificatePersistence;
import com.winllc.acme.server.persistence.ChallengePersistence;
import com.winllc.acme.server.persistence.OrderPersistence;
import com.winllc.acme.server.process.OrderProcessor;
import com.winllc.acme.server.service.internal.CertificateAuthorityService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.task.TaskExecutor;

import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CertIssuanceTransaction {

    private OrderPersistence orderPersistence;
    private AuthorizationPersistence authorizationPersistence;
    private ChallengePersistence challengePersistence;
    private CertificateAuthorityService certificateAuthorityService;
    private CertificatePersistence certificatePersistence;
    private TaskExecutor taskExecutor;

    private CertificateAuthority ca;

    protected UUID transactionId;
    private AccountData accountData;
    private DirectoryData directoryData;

    private OrderRequest initialOrderRequest;
    private OrderDataWrapper orderDataWrapper;

    public CertIssuanceTransaction(){
        this.transactionId = UUID.randomUUID();
    }

    public void load(OrderPersistence orderPersistence, AuthorizationPersistence authorizationPersistence,
                     ChallengePersistence challengePersistence, CertificateAuthorityService certificateAuthorityService,
                     CertificatePersistence certificatePersistence, TaskExecutor taskExecutor) {
        this.orderPersistence = orderPersistence;
        this.authorizationPersistence = authorizationPersistence;
        this.challengePersistence = challengePersistence;
        this.certificateAuthorityService = certificateAuthorityService;
        this.certificatePersistence = certificatePersistence;
        this.taskExecutor = taskExecutor;
    }

    public AccountData getAccountData() {
        return accountData;
    }

    public DirectoryData getDirectoryData() {
        return directoryData;
    }

    public void init(AccountData accountData, DirectoryData directoryData) {
        this.accountData = accountData;
        this.directoryData = directoryData;
        this.ca = certificateAuthorityService.getByName(directoryData.getMapsToCertificateAuthorityName());
    }

    public void startOrder(OrderRequest orderRequest) throws Exception {
        this.initialOrderRequest = orderRequest;
        this.orderDataWrapper = new OrderDataWrapper(this.initialOrderRequest);
    }

    public List<ChallengeData> retrieveCurrentChallenges() {
        List<ChallengeData> list = new ArrayList<>();
        this.orderDataWrapper.authorizationDataWrappers
                .forEach(a -> {
                    List<ChallengeData> challengeDataList = a.challengeDataWrappers.stream()
                            .map(w -> w.challengeData)
                            .collect(Collectors.toList());
                    list.addAll(challengeDataList);
                });

        return list;
    }

    public List<AuthorizationData> retrieveCurrentAuthorizations(){
        List<AuthorizationData> list = new ArrayList<>();
        return this.orderDataWrapper.authorizationDataWrappers.stream()
                .map(w -> w.authorizationData)
                .collect(Collectors.toList());
    }

    public AuthorizationData retrieveAuthorizationData(String id){
        return this.orderDataWrapper.authorizationDataWrappers.stream()
                .map(w -> w.authorizationData)
                .filter(a -> a.getId().contentEquals(id))
                .findFirst().get();
    }

    public ChallengeData retrieveChallengeData(String id){
        return this.orderDataWrapper.authorizationDataWrappers.stream()
                .map(w -> w.challengeDataWrappers)
                .flatMap(w -> w.stream())
                .map(w -> w.challengeData)
                .filter(a -> a.getId().contentEquals(id))
                .findFirst().get();
    }

    public void attemptChallenge(String challengeId){
        ChallengeData challengeData = retrieveChallengeData(challengeId);
        ChallengeType challengeType = ChallengeType.getValue(challengeData.getObject().getType());
        switch (challengeType){
            case HTTP:
                taskExecutor.execute(new HttpChallengeRunner.VerificationRunner(challengeId, this));
                break;
            case DNS:
                //todo
                break;
        }
    }

    public void markChallengeComplete(String challengeId) {
        for(AuthorizationDataWrapper wrapper : orderDataWrapper.authorizationDataWrappers){
            Optional<ChallengeDataWrapper> wrapperOptional = wrapper.challengeDataWrappers.stream()
                    .filter(w -> w.challengeData.getId().contentEquals(challengeId))
                    .findFirst();
            if(wrapperOptional.isPresent()){
                wrapperOptional.get().markComplete();
            }
        }
    }

    public StatusType getOrderStatus() {
        return this.orderDataWrapper.orderData.getObject().getStatusType();
    }

    public OrderData getOrderData(){
        updateOrderData();
        return this.orderDataWrapper.orderData;
    }

    public void finalizeOrder(String csr) throws Exception{
        if(this.orderDataWrapper.orderData.getObject().getStatusType() == StatusType.READY) {
            this.orderDataWrapper.updateStatus(StatusType.PROCESSING);
            this.orderDataWrapper.finalizeOrder(csr);
        }else{
            throw new Exception("Order not READY");
        }
    }

    private void updateOrderData(){
        List<String> authorizations = new ArrayList<>();
        for(AuthorizationDataWrapper authorizationDataWrapper : this.orderDataWrapper.authorizationDataWrappers){
            AuthorizationData authorizationData = authorizationDataWrapper.authorizationData;
            for(ChallengeDataWrapper challengeDataWrapper : authorizationDataWrapper.challengeDataWrappers){
                ChallengeData challengeData = challengeDataWrapper.challengeData;
                authorizationData.getObject().addChallenge(challengeData.getObject());
            }
            authorizationDataWrapper.persist();
            authorizations.add(authorizationData.buildUrl(Application.baseURL));
        }
        this.orderDataWrapper.orderData.getObject().setAuthorizations(authorizations.toArray(new String[0]));
        this.orderDataWrapper.persist();
    }

    class OrderDataWrapper {
        OrderData orderData;
        List<AuthorizationDataWrapper> authorizationDataWrappers;

        OrderDataWrapper(OrderRequest orderRequest) throws Exception {
            init(orderRequest);
        }

        void init(OrderRequest orderRequest) throws Exception {
            this.authorizationDataWrappers = new ArrayList<>();

            Order order = new Order();
            order.setStatus(StatusType.PENDING.toString());
            order.setIdentifiers(orderRequest.getIdentifiers());

            this.orderData = new OrderData(order, directoryData.getName(), accountData.getAccountId());
            this.orderData.getObject().setFinalize(orderData.buildUrl(Application.baseURL)+"/finalize");

            if (orderRequest.getIdentifiers() != null) {
                for (Identifier identifier : orderRequest.getIdentifiers()) {
                    try {
                        AuthorizationDataWrapper authorizationDataWrapper = new AuthorizationDataWrapper(this, identifier);
                        this.authorizationDataWrappers.add(authorizationDataWrapper);
                    } catch (AcmeServerException e) {
                        e.printStackTrace();
                    }
                }
            }

            persist();
        }

        Set<Identifier> getIdentifiers(){
            return Stream.of(this.orderData.getObject().getIdentifiers()).collect(Collectors.toSet());
        }

        private void updateStatus(StatusType statusType){
            //todo verify
            this.orderData.getObject().setStatus(statusType.toString());
            persist();
        }

        void markInvalid(){
            updateStatus(StatusType.INVALID);
        }

        void finalizeOrder(String csr) {
            taskExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        String eabKid = null;
                        if (StringUtils.isNotBlank(accountData.getEabKeyIdentifier())) {
                            eabKid = accountData.getEabKeyIdentifier();
                        }

                        X509Certificate certificate = ca.issueCertificate(getIdentifiers(), eabKid, CertUtil.csrBase64ToPKC10Object(csr));
                        String[] certWithChains = CertUtil.certAndChainsToPemArray(certificate, ca.getTrustChain());
                        CertData certData = new CertData(certWithChains, directoryData.getName(), accountData.getAccountId());
                        certData = certificatePersistence.save(certData);

                        orderData.getObject().setCertificate(certData.buildUrl(Application.baseURL));
                        orderData.getObject().setStatus(StatusType.VALID.toString());
                        persist();
                    } catch (Exception e) {
                        e.printStackTrace();
                        updateStatus(StatusType.INVALID);
                    }
                }
            });

        }

        void persist() {
            this.orderData.addTransactionId(transactionId);
            this.orderData = orderPersistence.save(this.orderData);
        }
    }

    class AuthorizationDataWrapper {
        OrderDataWrapper orderDataWrapper;
        AuthorizationData authorizationData;
        List<ChallengeDataWrapper> challengeDataWrappers;

        AuthorizationDataWrapper(OrderDataWrapper orderDataWrapper, Identifier identifier) throws Exception {
            this.orderDataWrapper = orderDataWrapper;
            init(identifier);
        }

        void init(Identifier identifier) throws Exception {
            IdentifierWrapper identifierWrapper = new IdentifierWrapper(identifier);
            if (identifierWrapper.canIssue()) {
                this.challengeDataWrappers = new ArrayList<>();

                Authorization authorization = new Authorization();
                authorization.markPending();
                authorization.setIdentifier(identifier);

                this.authorizationData = new AuthorizationData(authorization, directoryData.getName());

                List<ChallengeType> identifierChallengeRequirements =
                        ca.getIdentifierChallengeRequirements(identifier, accountData, directoryData);

                if (identifierChallengeRequirements != null) {
                    for (ChallengeType challengeType : identifierChallengeRequirements) {
                        ChallengeDataWrapper challengeDataWrapper = new ChallengeDataWrapper(this, challengeType);
                        this.authorizationData.getObject().addChallenge(challengeDataWrapper.challengeData.getObject());
                        this.challengeDataWrappers.add(challengeDataWrapper);
                    }
                }

                if(this.challengeDataWrappers.size() == 0){
                    this.authorizationData.getObject().setStatus(StatusType.VALID.toString());
                    persist();
                    orderDataWrapper.updateStatus(StatusType.READY);
                }else{
                    persist();
                }

            } else {
                throw new Exception("Cannot issue to: " + identifier.getValue());
            }
        }

        void challengeCompleted(){
            int total = challengeDataWrappers.size();
            int complete = 0;
            for(ChallengeDataWrapper wrapper : challengeDataWrappers){
                if(wrapper.challengeData.getObject().getStatusType() == StatusType.VALID) complete++;
            }

            if(complete == total){
                orderDataWrapper.updateStatus(StatusType.READY);
            }else{
                System.out.println("All challenges not complete");
            }
        }

        AuthorizationData get() {
            return authorizationData;
        }

        void persist() {
            this.authorizationData.addTransactionId(transactionId);
            this.authorizationData = authorizationPersistence.save(this.authorizationData);
        }
    }

    class ChallengeDataWrapper {
        AuthorizationDataWrapper authorizationData;
        ChallengeData challengeData;

        ChallengeDataWrapper(AuthorizationDataWrapper authorizationData, ChallengeType challengeType) {
            this.authorizationData = authorizationData;
            Challenge challenge = new Challenge();
            challenge.markPending();

            String token = RandomStringUtils.random(50);

            Base64.Encoder urlEncoder = java.util.Base64.getUrlEncoder().withoutPadding();
            String encoded = urlEncoder.encodeToString(token.getBytes());
            challenge.setToken(encoded);
            challenge.setStatus(StatusType.PENDING.toString());

            this.challengeData = new ChallengeData(challenge, directoryData.getName());
            this.challengeData.setAuthorizationId(authorizationData.authorizationData.getId());

            this.challengeData.getObject().setUrl(this.challengeData.buildUrl(Application.baseURL));
            this.challengeData.getObject().setType(challengeType.toString());

            persist();
        }

        void markComplete(){
            this.challengeData.getObject().setStatus(StatusType.VALID.toString());
            this.authorizationData.challengeCompleted();
        }

        void persist() {
            this.challengeData.addTransactionId(transactionId);
            this.challengeData = challengePersistence.save(this.challengeData);
        }
    }

    class CertDataWrapper {
        CertData certData;

        CertDataWrapper(){

        }

        void issueCertificate(){

        }

        void persist(){

        }
    }

    class IdentifierWrapper {
        Identifier identifier;

        IdentifierWrapper(Identifier identifier) {
            this.identifier = identifier;
        }

        boolean canIssue() throws AcmeServerException {
            return ca.canIssueToIdentifier(identifier, accountData, directoryData);
        }
    }
}
