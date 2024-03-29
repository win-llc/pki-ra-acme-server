package com.winllc.acme.server.external;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.util.Base64URL;
import com.winllc.acme.common.CertIssuanceValidationResponse;
import com.winllc.acme.common.ExternalAccountProviderSettings;
import com.winllc.acme.common.constants.ProblemType;
import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.util.HttpCommandUtil;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.exceptions.InternalServerException;
import org.apache.http.HttpException;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class WINLLCExternalAccountProvider implements ExternalAccountProvider {

    private static final Logger log = LogManager.getLogger(WINLLCExternalAccountProvider.class);

    private String name;
    private String baseUrl;
    private String accountVerificationUrl = "/validation/account/verify";
    private String accountValidationRulesUrl = "/validation/rules";

    public WINLLCExternalAccountProvider(ExternalAccountProviderSettings settings){
        this.name = settings.getName();
        //this.accountVerificationUrl = settings.getAccountVerificationUrl();
        //this.accountValidationRulesUrl = settings.getAccountValidationRulesUrl();
        this.baseUrl = settings.getBaseUrl();
    }

    @Override
    public String getName() {
        return name;
    }



    @Override
    public List<String> getCanIssueToDomainsForExternalAccount(String accountKeyIdentifier) throws Exception {
        String url = baseUrl+"/validation/account/getCanIssueDomains/"+accountKeyIdentifier;

        return HttpCommandUtil.process(new HttpGet(url), 200, List.class);
    }

    @Override
    public String getAccountVerificationUrl() {
        return baseUrl+accountVerificationUrl;
    }

    @Override
    public String getAccountValidationRulesUrl() {
        return baseUrl+accountValidationRulesUrl;
    }

    @Override
    public List<String> getPreAuthorizationIdentifiers(String accountKeyIdentifier) throws InternalServerException {
        //todo

        String url = baseUrl+"/account/preAuthzIdentifiers/"+accountKeyIdentifier;

        try {
            return HttpCommandUtil.process(new HttpGet(url), 200, List.class);
        } catch (Exception e) {
            log.error("Could not retrieve pre-authz", e);
            throw new InternalServerException("Could not retrieve pre-authz", e);
        }
    }

    @Override
    public CertIssuanceValidationResponse getValidationRules(AccountData accountData) throws AcmeServerException {
        String verificationUrl = getAccountValidationRulesUrl()+"/"+accountData.getEabKeyIdentifier();

        try {
            HttpPost httppost = new HttpPost(verificationUrl);

            return HttpCommandUtil.process(httppost, 200, CertIssuanceValidationResponse.class);
        }catch (HttpException e){
            log.error("Did not receive expected return code");
            throw new AcmeServerException(ProblemType.SERVER_INTERNAL, "Could not retrieve validation rules");
        }catch (Exception e){
            log.error("Could not get validation rules", e);
            throw new AcmeServerException(ProblemType.SERVER_INTERNAL, e.getMessage());
        }
    }

    /*
    To verify the account binding, the CA MUST take the following steps:

    Verify that the value of the field is a well-formed JWS
    Verify that the JWS protected field meets the above criteria
    Retrieve the MAC key corresponding to the key identifier in the “kid” field
    Verify that the MAC on the JWS verifies using that MAC key
    Verify that the payload of the JWS represents the same key as was used to verify the outer JWS (i.e., the “jwk” field of the outer JWS)
     */
    public boolean verifyAccountBinding(JWSObject jwsObject, JWSObject outerJWSObject) throws AcmeServerException {
        //Send JWS to verificationURL
        String url = getAccountVerificationUrl();
        Base64URL macKey = jwsObject.getSignature();
        String keyIdentifier = jwsObject.getHeader().getKeyID();

        HttpPost httppost = new HttpPost(url);

        // Request parameters and other properties.
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("keyIdentifier", keyIdentifier));
        params.add(new BasicNameValuePair("macKey", macKey.toString()));
        params.add(new BasicNameValuePair("jwsObject", jwsObject.serialize()));
        params.add(new BasicNameValuePair("accountObject", outerJWSObject.serialize()));
        try {
            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

            HttpCommandUtil.process(httppost, 200, String.class);

            return true;
        }catch (Exception e){
            AcmeServerException exception = new AcmeServerException(ProblemType.SERVER_INTERNAL, "Could not verify external account");
            exception.addSuppressed(e);
            throw exception;
        }
    }
}
