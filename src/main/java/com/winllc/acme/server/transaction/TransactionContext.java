package com.winllc.acme.server.transaction;

import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.server.external.CertificateAuthority;
import com.winllc.acme.server.persistence.*;
import com.winllc.acme.server.service.internal.CertificateAuthorityService;
import org.springframework.core.task.TaskExecutor;

import java.util.UUID;

public class TransactionContext {
    private final AccountPersistence accountPersistence;
    private final OrderPersistence orderPersistence;
    private final AuthorizationPersistence authorizationPersistence;
    private final ChallengePersistence challengePersistence;
    private final CertificateAuthorityService certificateAuthorityService;
    private final CertificatePersistence certificatePersistence;
    private final TaskExecutor taskExecutor;

    protected UUID transactionId;
    private final AccountData accountData;
    private final DirectoryData directoryData;
    private final CertificateAuthority ca;

    public TransactionContext(AccountPersistence accountPersistence, OrderPersistence orderPersistence, AuthorizationPersistence authorizationPersistence,
                              ChallengePersistence challengePersistence, CertificateAuthorityService certificateAuthorityService,
                              CertificatePersistence certificatePersistence, TaskExecutor taskExecutor, UUID transactionId,
                              AccountData accountData, DirectoryData directoryData) {
        this.accountPersistence = accountPersistence;
        this.orderPersistence = orderPersistence;
        this.authorizationPersistence = authorizationPersistence;
        this.challengePersistence = challengePersistence;
        this.certificateAuthorityService = certificateAuthorityService;
        this.certificatePersistence = certificatePersistence;
        this.taskExecutor = taskExecutor;
        this.transactionId = transactionId;
        this.accountData = accountData;
        this.directoryData = directoryData;

        this.ca = certificateAuthorityService.getByName(directoryData.getMapsToCertificateAuthorityName());
    }

    public TransactionContext(AccountPersistence accountPersistence, OrderPersistence orderPersistence, AuthorizationPersistence authorizationPersistence,
                              ChallengePersistence challengePersistence, CertificateAuthorityService certificateAuthorityService,
                              CertificatePersistence certificatePersistence, TaskExecutor taskExecutor,
                              AccountData accountData, DirectoryData directoryData){
        this(accountPersistence, orderPersistence, authorizationPersistence, challengePersistence,
                certificateAuthorityService, certificatePersistence,
                taskExecutor, UUID.randomUUID(), accountData, directoryData);
    }

    public OrderPersistence getOrderPersistence() {
        return orderPersistence;
    }

    public AuthorizationPersistence getAuthorizationPersistence() {
        return authorizationPersistence;
    }

    public ChallengePersistence getChallengePersistence() {
        return challengePersistence;
    }

    public CertificateAuthorityService getCertificateAuthorityService() {
        return certificateAuthorityService;
    }

    public CertificatePersistence getCertificatePersistence() {
        return certificatePersistence;
    }

    public TaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public AccountData getAccountData() {
        return accountData;
    }

    public DirectoryData getDirectoryData() {
        return directoryData;
    }

    public CertificateAuthority getCa() {
        return ca;
    }

    public AccountPersistence getAccountPersistence() {
        return accountPersistence;
    }
}
