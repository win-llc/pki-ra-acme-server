package com.winllc.acme.server.transaction;

import com.winllc.acme.common.model.data.CertData;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.acme.server.exceptions.AcmeServerException;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

class CertDataWrapper extends DataWrapper<CertData> {
    CertData certData;
    X509Certificate x509Certificate;

    public CertDataWrapper(TransactionContext transactionContext) {
        super(transactionContext);
    }


    void certIssued(X509Certificate certificate) throws AcmeServerException, CertificateEncodingException {
        this.x509Certificate = certificate;
        String[] certWithChains = CertUtil.certAndChainsToPemArray(certificate, transactionContext.getCa().getTrustChain());
        certData = new CertData(certWithChains, transactionContext.getDirectoryData().getName(),
                transactionContext.getAccountData().getId());
        persist();
    }

    void revoke(int reason) throws AcmeServerException {
        transactionContext.getCa().revokeCertificate(this.x509Certificate, reason);
    }

    @Override
    CertData getData() {
        return certData;
    }

    void persist(){
        certData = transactionContext.getCertificatePersistence().save(certData);
    }
}
