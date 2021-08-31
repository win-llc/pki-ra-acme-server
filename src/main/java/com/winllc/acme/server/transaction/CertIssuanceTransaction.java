package com.winllc.acme.server.transaction;

import com.winllc.acme.common.constants.StatusType;
import com.winllc.acme.common.model.data.*;
import com.winllc.acme.common.model.requestresponse.OrderRequest;
import com.winllc.acme.server.Application;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CertIssuanceTransaction extends AuthorizationTransaction {

    private OrderRequest initialOrderRequest;
    private OrderDataWrapper orderDataWrapper;

    CertIssuanceTransaction(TransactionContext transactionContext) {
        super(transactionContext);
    }

    public void load(TransactionContext transactionContext) {
        this.transactionContext = transactionContext;
    }

    public AccountData getAccountData() {
        return this.transactionContext.getAccountData();
    }

    public DirectoryData getDirectoryData() {
        return this.transactionContext.getDirectoryData();
    }

    /*
    public void reloadData(AccountData accountData, DirectoryData directoryData){
        this.orderDataWrapper.reload();
    }

     */

    public OrderData startOrder(OrderRequest orderRequest) throws Exception {
        this.initialOrderRequest = orderRequest;
        this.orderDataWrapper = new OrderDataWrapper(this.initialOrderRequest, transactionContext);
        updateOrderData();
        return this.orderDataWrapper.getData();
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
        return this.orderDataWrapper.authorizationDataWrappers.stream()
                .map(w -> w.authorizationData)
                .collect(Collectors.toList());
    }



    public ChallengeDataWrapper retrieveChallengeData(String id){
        return this.orderDataWrapper.authorizationDataWrappers.stream()
                .map(w -> w.challengeDataWrappers)
                .flatMap(w -> w.stream())
                .filter(a -> a.challengeData.getId().contentEquals(id))
                .findFirst().get();
    }

    public void markChallengeComplete(String challengeId) {
        for(AuthorizationDataWrapper wrapper : orderDataWrapper.authorizationDataWrappers){
            Optional<ChallengeDataWrapper> wrapperOptional = wrapper.challengeDataWrappers.stream()
                    .filter(w -> w.challengeData.getId().contentEquals(challengeId))
                    .findFirst();
            if(wrapperOptional.isPresent()){
                wrapperOptional.get().markValid();
            }
        }
    }

    public AuthorizationData retrieveAuthorizationData(String id){
        return this.orderDataWrapper.authorizationDataWrappers.stream()
                .map(w -> w.authorizationData)
                .filter(a -> a.getId().contentEquals(id))
                .findFirst().get();
    }

    public StatusType getOrderStatus() {
        return this.orderDataWrapper.orderData.getObject().getStatusType();
    }

    public OrderData getOrderData(){
        updateOrderData();
        return this.orderDataWrapper.getData();
    }

    public void finalizeOrder(String csr) throws Exception{
        if(this.orderDataWrapper.getStatus() == StatusType.READY) {
            this.orderDataWrapper.markProcessing();
            this.orderDataWrapper.finalizeOrder(csr);
        }else{
            throw new Exception("Order not READY");
        }
    }

    //todo cleanup
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


}
