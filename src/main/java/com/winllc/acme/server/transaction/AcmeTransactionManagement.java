package com.winllc.acme.server.transaction;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.winllc.acme.common.model.acme.Account;
import com.winllc.acme.common.model.acme.Directory;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.server.persistence.*;
import com.winllc.acme.server.service.internal.CertificateAuthorityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class AcmeTransactionManagement {

    @Autowired
    private OrderPersistence orderPersistence;
    @Autowired
    private AuthorizationPersistence authorizationPersistence;
    @Autowired
    private ChallengePersistence challengePersistence;
    @Autowired
    private CertificateAuthorityService certificateAuthorityService;
    @Autowired
    private CertificatePersistence certificatePersistence;
    @Autowired
    private AccountPersistence accountPersistence;
    @Autowired
    @Qualifier("appTaskExecutor")
    private TaskExecutor taskExecutor;

    private static final Cache<UUID, AbstractTransaction> sessionIdCurrentTransactionMap;

    static {
        sessionIdCurrentTransactionMap = CacheBuilder.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .removalListener(new RemovalListener<UUID, AbstractTransaction>() {
                    @Override
                    public void onRemoval(RemovalNotification<UUID, AbstractTransaction> removalNotification) {
                        System.out.println("Removing cached CertIssuanceTransaction: "+removalNotification.getKey());
                    }
                })
                .build();
    }

    public CertIssuanceTransaction startNewOrder(AccountData accountData, DirectoryData directoryData){
        TransactionContext transactionContext = new TransactionContext(accountPersistence, orderPersistence,
                authorizationPersistence, challengePersistence,
                certificateAuthorityService, certificatePersistence, taskExecutor, accountData, directoryData);

        CertIssuanceTransaction transaction = new CertIssuanceTransaction(transactionContext);
        sessionIdCurrentTransactionMap.put(transaction.transactionContext.transactionId, transaction);
        return transaction;
    }

    public PreAuthzTransaction startNewPreAuthz(AccountData accountData, DirectoryData directoryData){
        TransactionContext transactionContext = new TransactionContext(accountPersistence, orderPersistence,
                authorizationPersistence, challengePersistence,
                certificateAuthorityService, certificatePersistence, taskExecutor, accountData, directoryData);

        PreAuthzTransaction transaction = new PreAuthzTransaction(transactionContext);
        sessionIdCurrentTransactionMap.put(transaction.transactionContext.transactionId, transaction);
        return transaction;
    }

    public void updateTransaction(CertIssuanceTransaction transaction){
        sessionIdCurrentTransactionMap.put(transaction.transactionContext.transactionId, transaction);
    }

    public <T extends AbstractTransaction> T getTransaction(String transactionId, Class<T> clazz){
        UUID transactionUUID = UUID.fromString(transactionId);
        AbstractTransaction transaction = sessionIdCurrentTransactionMap.getIfPresent(transactionUUID);

        if(clazz.isInstance(transaction)){
            return (T) transaction;
        }else{
            throw new IllegalArgumentException("Invalid class type: "+clazz.getCanonicalName());
        }

    }

}
