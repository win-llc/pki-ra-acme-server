package com.winllc.acme.server.external;

import com.winllc.acme.common.*;
import com.winllc.acme.common.contants.ProblemType;
import com.winllc.acme.common.model.acme.Identifier;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.common.util.HttpCommandUtil;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.service.internal.ExternalAccountProviderService;
import com.winllc.ra.client.CertAuthorityConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.util.CollectionUtils;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;

public class WINLLCCertAuthority extends AbstractCertAuthority {

    private static final Logger log = LogManager.getLogger(WINLLCCertAuthority.class);

    private final String validationRulesPath = "/ca/validationRules/";

    private ExternalAccountProviderService externalAccountProviderService;

    public WINLLCCertAuthority(CertificateAuthoritySettings settings) {
        super(settings);
    }

    public WINLLCCertAuthority(CertificateAuthoritySettings settings, ExternalAccountProviderService service){
        super(settings);
        this.externalAccountProviderService = service;
    }

    @Override
    public boolean revokeCertificate(X509Certificate certificate, int reason) throws AcmeServerException {
        CertAuthorityConnection certAuthorityConnection
                = new CertAuthorityConnection(settings.getBaseUrl(), settings.getMapsToCaConnectionName());

        return certAuthorityConnection.revokeCertificate(certificate, reason);
    }

    @Override
    public X509Certificate issueCertificate(Collection<Identifier> identifiers, String eabKid, PKCS10CertificationRequest certificationRequest) throws AcmeServerException {
        CertAuthorityConnection certAuthorityConnection = new CertAuthorityConnection(settings.getBaseUrl(), settings.getMapsToCaConnectionName());

        try {
            X509Certificate certificate = certAuthorityConnection.issueCertificate(new HashSet<>(identifiers),
                    eabKid, certificationRequest, "acme");

            if(certificate != null){
                return certificate;
            }else{
                throw new Exception("Failed to build cert");
            }
        }catch (Exception e){
            log.error("Could not issuer cert", e);
        }

        throw new AcmeServerException(ProblemType.SERVER_INTERNAL, "Could not issue certificate");
    }

    @Override
    public Optional<CertificateDetails> getCertificateDetails(String serial) {
        CertAuthorityConnection certAuthorityConnection = new CertAuthorityConnection(settings.getBaseUrl(), settings.getMapsToCaConnectionName());

        try {
            return certAuthorityConnection.getCertificateDetails(serial);
        } catch (Exception e) {
            log.error("Could not process", e);
        }

        return Optional.empty();
    }

    @Override
    public CertRevocationStatus isCertificateRevoked(X509Certificate certificate) {
        CertAuthorityConnection certAuthorityConnection = new CertAuthorityConnection(settings.getBaseUrl(), settings.getMapsToCaConnectionName());

        return certAuthorityConnection.isCertificateRevoked(certificate);
    }

    @Override
    public Certificate[] getTrustChain() throws AcmeServerException {
        CertAuthorityConnection certAuthorityConnection = new CertAuthorityConnection(settings.getBaseUrl(), settings.getMapsToCaConnectionName());

        try {
            return certAuthorityConnection.getTrustChain();
        } catch (Exception e) {
            log.error("Could not process get", e);
            throw new AcmeServerException(ProblemType.SERVER_INTERNAL, "Could not retrieve trust chain");
        }
    }

    //Get rules applied to specified account from an external source
    @Override
    public CertIssuanceValidationResponse getValidationRules(AccountData accountData, DirectoryData directoryData) throws AcmeServerException {
        boolean externalAccountRequired = directoryData.getObject().getMeta().isExternalAccountRequired();

        CertIssuanceValidationResponse response = new CertIssuanceValidationResponse();

        if(externalAccountRequired) {
            if (StringUtils.isNotEmpty(directoryData.getExternalAccountProviderName())) {
                ExternalAccountProvider eap = externalAccountProviderService.findByName(directoryData.getExternalAccountProviderName());

                CertIssuanceValidationResponse validationRules = eap.getValidationRules(accountData);
                if(!CollectionUtils.isEmpty(validationRules.getCertIssuanceValidationRules())) {
                    response.getCertIssuanceValidationRules().addAll(validationRules.getCertIssuanceValidationRules());
                }
                response.setAccountIsValid(validationRules.isAccountIsValid());
            } else {
                response.setAccountIsValid(true);
                return response;
            }
        }

        Optional<CertIssuanceValidationResponse> optionalResponse = getCAGlobalValidationRules();
        if(optionalResponse.isPresent()){
            CertIssuanceValidationResponse globalResponse = optionalResponse.get();
            if(!CollectionUtils.isEmpty(globalResponse.getCertIssuanceValidationRules())) {
                response.getCertIssuanceValidationRules().addAll(globalResponse.getCertIssuanceValidationRules());
            }
        }else{
            log.info("No global CA rules found");
        }

        return response;
    }

    public Optional<CertIssuanceValidationResponse> getCAGlobalValidationRules() {
        String fullUrl = settings.getBaseUrl()+validationRulesPath+settings.getMapsToCaConnectionName();
        try {
            URIBuilder builder = new URIBuilder(fullUrl);
            HttpGet httpGet = new HttpGet(builder.build());

            CertIssuanceValidationResponse response = HttpCommandUtil.process(httpGet, 200, CertIssuanceValidationResponse.class);

            if(response != null){
                return Optional.of(response);
            }else{
                log.debug("Response was empty");
            }
        } catch (Exception e) {
            log.error("Could not process", e);
        }

        return Optional.empty();
    }

    @Override
    public boolean canIssueToIdentifier(Identifier identifier, AccountData accountData, DirectoryData directoryData) throws AcmeServerException {

        CertIssuanceValidationResponse validationResponse = getValidationRules(accountData, directoryData);

        //if account is not valid, do not issue
        if(!validationResponse.isAccountIsValid()) return false;
        //no rules, can issue
        if(validationResponse.getCertIssuanceValidationRules().size() == 0) return true;

        for (CertIssuanceValidationRule rule : validationResponse.getCertIssuanceValidationRules()) {
            if(canIssueToIdentifier(identifier, rule)){
                return true;
            }
        }
        return false;
    }

    public static List<String> getRequiredProperties() {
        List<String> props = new ArrayList<>();
        props.add("issueCertUrl");
        return props;
    }


}
