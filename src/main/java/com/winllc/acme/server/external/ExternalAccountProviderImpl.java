package com.winllc.acme.server.external;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import com.winllc.acme.server.model.AcmeJWSObject;
import com.winllc.acme.server.util.SecurityValidatorUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ExternalAccountProviderImpl implements ExternalAccountProvider {

    private String name;
    private String linkedDirectoryName;
    private String accountVerificationUrl;

    public ExternalAccountProviderImpl(String name, String linkedDirectoryName, String accountVerificationUrl) {
        this.name = name;
        this.linkedDirectoryName = linkedDirectoryName;
        this.accountVerificationUrl = accountVerificationUrl;
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
        AcmeJWSObject innerObject = SecurityValidatorUtil.getPayloadFromJWSObject(outerObject, AcmeJWSObject.class);

        JWSHeader header = outerObject.getHeader();

        //The “alg” field MUST indicate a MAC-based algorithm
        if(!JWSAlgorithm.Family.HMAC_SHA.contains(header.getAlgorithm())){
            return false;
        }

        //The “nonce” field MUST NOT be present
        if(outerObject.getNonce() != null){
            return false;
        }

        //The “url” field MUST be set to the same value as the outer JWS
        String innerUrl = innerObject.getHeaderAcmeUrl().toString();
        String outerUrl = outerObject.getHeaderAcmeUrl().toString();
        if(!innerUrl.equalsIgnoreCase(outerUrl)){
            return false;
        }

        //The “kid” field MUST contain the key identifier provided by the CA
        return verifyAccountBinding(innerObject);
    }

    @Override
    public String getAccountVerificationUrl() {
        return accountVerificationUrl;
    }

    /*
    To verify the account binding, the CA MUST take the following steps:

    Verify that the value of the field is a well-formed JWS
    Verify that the JWS protected field meets the above criteria
    Retrieve the MAC key corresponding to the key identifier in the “kid” field
    Verify that the MAC on the JWS verifies using that MAC key
    Verify that the payload of the JWS represents the same key as was used to verify the outer JWS (i.e., the “jwk” field of the outer JWS)
     */
    private boolean verifyAccountBinding(AcmeJWSObject jwsObject) throws AcmeServerException {
        //Send JWS to verificationURL
        String url = getAccountVerificationUrl();
        String macKey = jwsObject.getSignature().decodeToString();
        String keyIdentifier = jwsObject.getHeader().getKeyID();

        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(url);

        // Request parameters and other properties.
        List<NameValuePair> params = new ArrayList<>(2);
        params.add(new BasicNameValuePair("keyIdentifier", keyIdentifier));
        params.add(new BasicNameValuePair("macKey", macKey));
        try {
            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

            //Execute and get the response.
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                if(response.getStatusLine().getStatusCode() == 200){
                    return true;
                }
                try (InputStream instream = entity.getContent()) {
                    //TODO do something useful, return true or false
                }
            }
        }catch (Exception e){
            AcmeServerException exception = new AcmeServerException(ProblemType.SERVER_INTERNAL, "Could not verify external account");
            exception.addSuppressed(e);
            throw exception;
        }

        return false;
    }
}
