package com.winllc.acme.server.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.acme.common.AdditionalSetting;
import com.winllc.acme.common.CAValidationRule;
import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.acme.server.contants.ChallengeType;
import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.model.acme.Identifier;
import com.winllc.acme.server.model.acme.ProblemDetails;
import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.OrderData;
import com.winllc.acme.server.service.internal.ExternalAccountProviderService;
import com.winllc.acme.server.util.HttpCommandUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.function.Function;

public class WINLLCCertAuthority extends AbstractCertAuthority {

    private static final Logger log = LogManager.getLogger(WINLLCCertAuthority.class);

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
        Optional<AdditionalSetting> optionalSetting = settings.getAdditionalSettingByKey("revokeCertUrl");

        String revokeCertUrl = optionalSetting.get().getValue();

        HttpPost httppost = new HttpPost(revokeCertUrl);

        try {
            List<NameValuePair> params = new ArrayList<>(2);
            params.add(new BasicNameValuePair("serial", certificate.getSerialNumber().toString()));
            params.add(new BasicNameValuePair("reason", Integer.toString(reason)));

            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

            HttpCommandUtil.process(httppost, 200, String.class);

            return true;
        }catch (Exception e){
            log.error("Could not issuer cert", e);
            return false;
        }
    }

    @Override
    public X509Certificate issueCertificate(OrderData orderData, PKCS10CertificationRequest certificationRequest) throws AcmeServerException {

        Optional<AdditionalSetting> optionalSetting = settings.getAdditionalSettingByKey("issueCertUrl");

        String issueCertUrl = optionalSetting.get().getValue();

        Function<String, X509Certificate> processCert = (content) -> {
            try {
                return CertUtil.base64ToCert(content);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        };

        HttpPost httppost = new HttpPost(issueCertUrl);

        try {
            List<NameValuePair> params = new ArrayList<>(2);
            params.add(new BasicNameValuePair("pkcs10", CertUtil.certificationRequestToPEM(certificationRequest)));

            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

            X509Certificate certificate = HttpCommandUtil.processCustom(httppost, 200, processCert);

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
    public boolean isCertificateRevoked(X509Certificate certificate) {
        //todo
        return false;
    }

    @Override
    public Certificate[] getTrustChain() throws AcmeServerException {
        //todo, internal should not be static
        String url = settings.getBaseUrl()+"/ca/trustChain/internal";

        Function<String, Certificate[]> processTrustChain = (content) -> {
            try {
                return CertUtil.trustChainStringToCertArray(content);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        };

        HttpGet httpGet = new HttpGet(url);

        try {
            return HttpCommandUtil.processCustom(httpGet, 200, processTrustChain);
        } catch (AcmeServerException e) {
            e.printStackTrace();
            throw new AcmeServerException(ProblemType.SERVER_INTERNAL, "Could not retrieve trust chain");
        }

    }

    //Get rules applied to specified account from an external source
    @Override
    public List<CAValidationRule> getValidationRules(AccountData accountData) {
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
                    ArrayList list = objectMapper.readValue(validationRules, ArrayList.class);

                    List<CAValidationRule> rules = new ArrayList<>();

                    for(Object obj : list) {
                        rules.add(objectMapper.convertValue(obj, CAValidationRule.class));
                    }

                    return rules;
                }else{
                    log.error("Did not receive expected return code: "+response.getStatusLine().getStatusCode());
                }
            }
        }catch (Exception e){
            log.error("Could not get validation rules", e);
        }finally {
            httppost.completed();
        }

        return new ArrayList<>();
    }

    @Override
    public boolean canIssueToIdentifier(Identifier identifier, AccountData accountData) {
        //no rules, can issue
        if(getValidationRules(accountData).size() == 0) return true;

        for (CAValidationRule rule : getValidationRules(accountData)) {
            if(canIssueToIdentifier(identifier, rule)){
                return true;
            }
        }
        return false;
    }

    @Override
    public List<ChallengeType> getIdentifierChallengeRequirements(Identifier identifier, AccountData accountData) {
        Set<ChallengeType> challengeTypes = new HashSet<>();
        if(canIssueToIdentifier(identifier, accountData)){
            for (CAValidationRule rule : getValidationRules(accountData)) {
                if(canIssueToIdentifier(identifier, rule)){
                    if(rule.isRequireHttpChallenge()) challengeTypes.add(ChallengeType.HTTP);
                    if(rule.isRequireDnsChallenge()) challengeTypes.add(ChallengeType.DNS);
                }
            }
            return new ArrayList<>(challengeTypes);
        }
        return null;
    }

    public boolean canIssueToIdentifier(Identifier identifier, CAValidationRule validationRule){
        if(!identifier.getValue().contains(".") && validationRule.isAllowHostnameIssuance()){
            return true;
        }

        if(StringUtils.isNotBlank(validationRule.getIdentifierType()) && StringUtils.isNotBlank(validationRule.getBaseDomainName()) &&
                identifier.getType().contentEquals(validationRule.getIdentifierType()) && identifier.getValue().endsWith(validationRule.getBaseDomainName())){
            return validationRule.isAllowIssuance();
        }else{
            return false;
        }
    }
}
