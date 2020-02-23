package com.winllc.acme.server.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.acme.common.*;
import com.winllc.acme.common.ra.RACertificateIssueRequest;
import com.winllc.acme.common.ra.RACertificateRevokeRequest;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.acme.server.contants.ChallengeType;
import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.model.acme.Identifier;
import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.OrderData;
import com.winllc.acme.server.service.acme.AccountService;
import com.winllc.acme.server.service.internal.ExternalAccountProviderService;
import com.winllc.acme.common.util.HttpCommandUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
                e.printStackTrace();
            }
            return false;
        };

        try {
            RACertificateRevokeRequest revokeRequest = new RACertificateRevokeRequest(settings.getMapsToCaConnectionName());
            revokeRequest.setReason(reason);
            revokeRequest.setSerial(certificate.getSerialNumber().toString());

            return HttpCommandUtil.processCustomJsonPost(fullUrl, revokeRequest, 200, processReturn);
        }catch (Exception e){
            log.error("Could not issuer cert", e);
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
                e.printStackTrace();
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
    public AccountValidationResponse getValidationRules(AccountData accountData) throws AcmeServerException {
        ExternalAccountProvider eap = externalAccountProviderService.findByName(settings.getMapsToExternalAccountProviderName());
        String verificationUrl = eap.getAccountValidationRulesUrl()+"/"+accountData.getEabKeyIdentifier();

        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(verificationUrl);

        try {
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                if(response.getStatusLine().getStatusCode() == 200){
                    String validationRules = IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8.name());
                    ObjectMapper objectMapper = new ObjectMapper();
                    AccountValidationResponse validationResponse = objectMapper.readValue(validationRules, AccountValidationResponse.class);

                    return validationResponse;
                }else{
                    log.error("Did not receive expected return code: "+response.getStatusLine().getStatusCode());
                    throw new AcmeServerException(ProblemType.SERVER_INTERNAL, "Could not retrieve validation rules");
                }
            }
        }catch (Exception e){
            log.error("Could not get validation rules", e);
            throw new AcmeServerException(ProblemType.SERVER_INTERNAL, e.getMessage());
        }finally {
            httppost.completed();
        }

        throw new AcmeServerException(ProblemType.SERVER_INTERNAL, "Did not get valid response from validation server");
    }

    @Override
    public boolean canIssueToIdentifier(Identifier identifier, AccountData accountData) throws AcmeServerException {

        AccountValidationResponse validationResponse = getValidationRules(accountData);

        //if account is not valid, do not issue
        if(!validationResponse.isAccountIsValid()) return false;
        //no rules, can issue
        if(validationResponse.getCaValidationRules().size() == 0) return true;

        for (CAValidationRule rule : validationResponse.getCaValidationRules()) {
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
