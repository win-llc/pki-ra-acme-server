package com.winllc.acme.server.external;

import com.winllc.acme.common.CAValidationRule;
import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.server.contants.ChallengeType;
import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.model.acme.Identifier;
import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.OrderData;
import com.winllc.acme.server.util.CertUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WINLLCCertAuthority extends AbstractCertAuthority {

    public WINLLCCertAuthority(CertificateAuthoritySettings settings) {
        super(settings);
    }

    @Override
    public boolean revokeCertificate(X509Certificate certificate, int reason) {
        //todo
        return false;
    }

    @Override
    public X509Certificate issueCertificate(OrderData orderData, PKCS10CertificationRequest certificationRequest) {
        //todo

        Map<String, String> additionalSettings = settings.getAdditionalSettings();

        String caUrl = additionalSettings.get("caUrl");

        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(caUrl);

        try {
            List<NameValuePair> params = new ArrayList<>(2);
            params.add(new BasicNameValuePair("pkcs10", CertUtil.certificationRequestToPEM(certificationRequest)));

            //Execute and get the response.
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                if(response.getStatusLine().getStatusCode() == 200){
                    //todo
                }
                try (InputStream instream = entity.getContent()) {
                    //TODO do something useful, return true or false
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            httppost.completed();
        }

        //todo submit csr to externalCA

        return null;
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
        //todo
        return new ArrayList<>();
    }

    @Override
    public boolean canIssueToIdentifier(Identifier identifier, AccountData accountData) {
        //todo
        return true;
    }

    @Override
    public List<ChallengeType> getIdentifierChallengeRequirements(Identifier identifier, AccountData accountData) {
        //todo
        return new ArrayList<>();
    }
}
