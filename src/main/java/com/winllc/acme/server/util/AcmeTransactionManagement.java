package com.winllc.acme.server.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.winllc.acme.server.persistence.AuthorizationPersistence;
import com.winllc.acme.server.persistence.CertificatePersistence;
import com.winllc.acme.server.persistence.ChallengePersistence;
import com.winllc.acme.server.persistence.OrderPersistence;
import com.winllc.acme.server.service.internal.CertificateAuthorityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    @Qualifier("appTaskExecutor")
    private TaskExecutor taskExecutor;

    private static final Cache<UUID, CertIssuanceTransaction> sessionIdCurrentTransactionMap;

    static {
        sessionIdCurrentTransactionMap = CacheBuilder.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .removalListener(new RemovalListener<UUID, CertIssuanceTransaction>() {
                    @Override
                    public void onRemoval(RemovalNotification<UUID, CertIssuanceTransaction> removalNotification) {
                        System.out.println("Removing cached CertIssuanceTransaction: "+removalNotification.getKey());
                    }
                })
                .build();
    }


    public CertIssuanceTransaction startNew(){
        CertIssuanceTransaction transaction = new CertIssuanceTransaction();
        transaction.load(orderPersistence, authorizationPersistence, challengePersistence,
                certificateAuthorityService, certificatePersistence, taskExecutor);
        sessionIdCurrentTransactionMap.put(transaction.transactionId, transaction);
        return transaction;
    }

    public void updateTransaction(CertIssuanceTransaction transaction){
        sessionIdCurrentTransactionMap.put(transaction.transactionId, transaction);
    }

    public CertIssuanceTransaction getTransaction(String transactionId){
        UUID transactionUUID = UUID.fromString(transactionId);
        CertIssuanceTransaction transaction = sessionIdCurrentTransactionMap.getIfPresent(transactionUUID);

        if(transaction != null) {
            transaction.load(orderPersistence, authorizationPersistence, challengePersistence,
                    certificateAuthorityService, certificatePersistence, taskExecutor);
        }
        return transaction;
    }

}
