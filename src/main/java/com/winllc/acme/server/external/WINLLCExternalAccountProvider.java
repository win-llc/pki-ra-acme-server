package com.winllc.acme.server.external;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.util.Base64URL;
import com.winllc.acme.common.ExternalAccountProviderSettings;
import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.model.AcmeJWSObject;
import com.winllc.acme.server.model.requestresponse.AccountRequest;
import com.winllc.acme.server.util.HttpCommandUtil;
import com.winllc.acme.server.util.SecurityValidatorUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class WINLLCExternalAccountProvider implements ExternalAccountProvider {

    private String name;
    private String linkedDirectoryName;
    private String baseUrl;
    private String accountVerificationUrl;
    private String accountValidationRulesUrl;

    public WINLLCExternalAccountProvider(ExternalAccountProviderSettings settings){
        this.name = settings.getName();
        this.accountVerificationUrl = settings.getAccountVerificationUrl();
        this.accountValidationRulesUrl = settings.getAccountValidationRulesUrl();
        this.baseUrl = settings.getBaseUrl();
    }

    @Override
    public String getName() {
        return name;
    }

    /*
        The ACME client then computes a binding JWS to indicate the external account holder’s approval of the ACME account key.
        The payload of this JWS is the ACME account key being registered, in JWK form. The protected header of the JWS MUST meet the following criteria:

    The “alg” field MUST indicate a MAC-based algorithm
    The “kid” field MUST contain the key identifier provided by the CA
    The “nonce” field MUST NOT be present
    The “url” field MUST be set to the same value as the outer JWS
         */
    @Override
    public boolean verifyExternalAccountJWS(AcmeJWSObject outerObject) throws AcmeServerException {
        AccountRequest innerObjectString = SecurityValidatorUtil.getPayloadFromJWSObject(outerObject, AccountRequest.class);
        JWSObject innerObject = null;
        try {
            innerObject = innerObjectString.buildExternalAccountJWSObject();
        } catch (ParseException e) {
            throw new AcmeServerException(ProblemType.SERVER_INTERNAL);
        }

        JWSHeader header = innerObject.getHeader();

        //The “alg” field MUST indicate a MAC-based algorithm
        if(!JWSAlgorithm.Family.HMAC_SHA.contains(header.getAlgorithm())){
            return false;
        }

        //The “nonce” field MUST NOT be present
        if(header.getCustomParam("nonce") != null){
            return false;
        }

        //The “url” field MUST be set to the same value as the outer JWS
        String innerUrl = innerObject.getHeader().getCustomParam("url").toString();
        String outerUrl = outerObject.getHeaderAcmeUrl().toString();
        if(!innerUrl.equalsIgnoreCase(outerUrl)){
            return false;
        }

        //The “kid” field MUST contain the key identifier provided by the CA
        return verifyAccountBinding(innerObject, outerObject);
    }

    @Override
    public List<String> getCanIssueToDomainsForExternalAccount(String accountKeyIdentifier) {
        String url = baseUrl+"/validation/account/getCanIssueDomains/"+accountKeyIdentifier;

        try {
            return HttpCommandUtil.process(new HttpGet(url), 200, List.class);
        } catch (AcmeServerException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String getAccountVerificationUrl() {
        return accountVerificationUrl;
    }

    @Override
    public String getAccountValidationRulesUrl() {
        return accountValidationRulesUrl;
    }

    /*
    To verify the account binding, the CA MUST take the following steps:

    Verify that the value of the field is a well-formed JWS
    Verify that the JWS protected field meets the above criteria
    Retrieve the MAC key corresponding to the key identifier in the “kid” field
    Verify that the MAC on the JWS verifies using that MAC key
    Verify that the payload of the JWS represents the same key as was used to verify the outer JWS (i.e., the “jwk” field of the outer JWS)
     */
    private boolean verifyAccountBinding(JWSObject jwsObject, JWSObject outerJWSObject) throws AcmeServerException {
        //Send JWS to verificationURL
        String url = getAccountVerificationUrl();
        Base64URL macKey = jwsObject.getSignature();
        String keyIdentifier = jwsObject.getHeader().getKeyID();

        HttpPost httppost = new HttpPost(url);

        // Request parameters and other properties.
        List<NameValuePair> params = new ArrayList<>(2);
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
