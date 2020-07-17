package com.winllc.acme.server.external;

import com.winllc.acme.common.*;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.acme.common.ra.RACertificateIssueRequest;
import com.winllc.acme.common.ra.RACertificateRevokeRequest;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.acme.common.contants.ProblemType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.common.model.acme.Identifier;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.OrderData;
import com.winllc.acme.server.service.internal.ExternalAccountProviderService;
import com.winllc.acme.common.util.HttpCommandUtil;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WINLLCCertAuthority extends AbstractCertAuthority {

    private static final Logger log = LogManager.getLogger(WINLLCCertAuthority.class);

    private final String issueCertPath = "/ca/issueCertificate";
    private final String revokeCertPath = "/ca/revokeCertificate";
    private final String trustChainPath = "/ca/trustChain/";
    private final String certDetailsPath = "/ca/certDetails/";
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
        String fullUrl = settings.getBaseUrl()+revokeCertPath;

        Function<String, Boolean> processReturn = (content) -> {
            try {
                return true;
            } catch (Exception e) {
                log.error("Could not process revoke cert", e);
            }
            return false;
        };

        try {
            RACertificateRevokeRequest revokeRequest = new RACertificateRevokeRequest(settings.getMapsToCaConnectionName());
            revokeRequest.setReason(reason);
            revokeRequest.setSerial(certificate.getSerialNumber().toString());

            return HttpCommandUtil.processCustomJsonPost(fullUrl, revokeRequest, 200, processReturn);
        }catch (Exception e){
            log.error("Could not revoke cert", e);
            return false;
        }
    }

    @Override
    public X509Certificate issueCertificate(OrderData orderData, String eabKid, PKCS10CertificationRequest certificationRequest) throws AcmeServerException {
        String fullUrl = settings.getBaseUrl()+issueCertPath;

        Function<String, X509Certificate> processCert = (content) -> {
            try {
                return CertUtil.base64ToCert(content);
            } catch (Exception e) {
                log.error("Could not covert base64 cert", e);
            }
            return null;
        };

        try {
            String csr = CertUtil.certificationRequestToPEM(certificationRequest);
            String dnsNames = Stream.of(orderData.getObject().getIdentifiers())
                    .map(Identifier::getValue)
                    .collect(Collectors.joining(","));

            RACertificateIssueRequest raCertificateRequest = new RACertificateIssueRequest(eabKid, csr, dnsNames, settings.getMapsToCaConnectionName());

            X509Certificate certificate = HttpCommandUtil.processCustomJsonPost(fullUrl, raCertificateRequest, 200, processCert);

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
        String fullUrl = settings.getBaseUrl()+certDetailsPath+settings.getMapsToCaConnectionName();
        try {
            URIBuilder builder = new URIBuilder(fullUrl);
            builder.setParameter("serial", serial);
            HttpGet httpGet = new HttpGet(builder.build());

            CertificateDetails details = HttpCommandUtil.process(httpGet, 200, CertificateDetails.class);

            if(details != null){
                return Optional.of(details);
            }

        } catch (Exception e) {
            log.error("Could not process", e);
        }

        return Optional.empty();
    }

    @Override
    public CertRevocationStatus isCertificateRevoked(X509Certificate certificate) {
        Optional<CertificateDetails> optionalCertificateDetails = getCertificateDetails(certificate.getSerialNumber().toString());
        if(optionalCertificateDetails.isPresent()){
            CertificateDetails certificateDetails = optionalCertificateDetails.get();
            return certificateDetails.getStatus().equalsIgnoreCase("REVOKED") ? CertRevocationStatus.REVOKED : CertRevocationStatus.VALID;
        }

        return CertRevocationStatus.UNKNOWN;
    }

    @Override
    public Certificate[] getTrustChain() throws AcmeServerException {
        String url = settings.getBaseUrl()+trustChainPath+settings.getMapsToCaConnectionName();

        Function<String, Certificate[]> processTrustChain = (content) -> {
            try {
                return CertUtil.trustChainStringToCertArray(content);
            } catch (Exception e) {
                log.error("Conversion failed", e);
            }
            return null;
        };

        HttpGet httpGet = new HttpGet(url);

        try {
            return HttpCommandUtil.processCustom(httpGet, 200, processTrustChain);
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
