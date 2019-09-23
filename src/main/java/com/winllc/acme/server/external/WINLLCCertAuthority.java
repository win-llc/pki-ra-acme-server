package com.winllc.acme.server.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.acme.common.CAValidationRule;
import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.acme.server.contants.ChallengeType;
import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.model.acme.Identifier;
import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.OrderData;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;

public class WINLLCCertAuthority extends AbstractCertAuthority {

    private static final Logger log = LogManager.getLogger(WINLLCCertAuthority.class);

    public WINLLCCertAuthority(CertificateAuthoritySettings settings) {
        super(settings);
    }

    @Override
    public boolean revokeCertificate(X509Certificate certificate, int reason) {
        //todo
        return false;
    }

    @Override
    public X509Certificate issueCertificate(OrderData orderData, PKCS10CertificationRequest certificationRequest) throws AcmeServerException {
        //todo

        Map<String, String> additionalSettings = settings.getAdditionalSettings();

        String caUrl = additionalSettings.get("caUrl");

        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(caUrl);

        try {
            List<NameValuePair> params = new ArrayList<>(2);
            params.add(new BasicNameValuePair("pkcs10", CertUtil.certificationRequestToPEM(certificationRequest)));

            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            //Execute and get the response.
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();


            if (entity != null) {
                if(response.getStatusLine().getStatusCode() == 200){
                    String b64Cert = IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8.name());
                    return CertUtil.base64ToCert(b64Cert);
                }
            }
        }catch (Exception e){
            log.error("Could not issuer cert", e);
        }finally {
            httppost.completed();
        }

        //todo submit csr to externalCA

        throw new AcmeServerException(ProblemType.SERVER_INTERNAL, "Could not issue certificate");
    }

    @Override
    public boolean isCertificateRevoked(X509Certificate certificate) {
        //todo
        return false;
    }

    @Override
    public Certificate[] getTrustChain() {
        //todo
        return new Certificate[0];
    }

    @Override
    public List<CAValidationRule> getValidationRules(AccountData accountData) {
        //todo not static
        String verificationUrl = settings.getExternalValidationRulesUrl()+"/"+"kidtest";

        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(verificationUrl);

        try {
            List<NameValuePair> params = new ArrayList<>(2);

            //httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            //Execute and get the response.
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();


            if (entity != null) {
                if(response.getStatusLine().getStatusCode() == 200){
                    //todo
                }

                String validationRules = IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8.name());
                ObjectMapper objectMapper = new ObjectMapper();
                ArrayList list = objectMapper.readValue(validationRules, ArrayList.class);

                List<CAValidationRule> rules = new ArrayList<>();

                for(Object obj : list) {
                    rules.add(objectMapper.convertValue(obj, CAValidationRule.class));
                }

                return rules;
            }
        }catch (Exception e){
            e.printStackTrace();
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
