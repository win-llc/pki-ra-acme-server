package com.winllc.acme.server.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.acme.server.contants.ProblemType;
import com.winllc.acme.server.exceptions.AcmeServerException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.HttpClients;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class HttpCommandUtil {

    public static <T> T process(HttpRequestBase request, int successCode, Class<T> returnClass) throws AcmeServerException {
        HttpClient httpclient = HttpClients.createDefault();

        try {
            //Execute and get the response.
            HttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                if(response.getStatusLine().getStatusCode() == successCode){
                    ObjectMapper objectMapper = new ObjectMapper();
                    String result = IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8.name());
                    return objectMapper.readValue(result, returnClass);
                }else{
                    throw new AcmeServerException(ProblemType.SERVER_INTERNAL, "Invalid response value: "+response.getStatusLine().getStatusCode());
                }
            }else{
                throw new AcmeServerException(ProblemType.SERVER_INTERNAL, "Http Entity was null");
            }
        }catch (Exception e){
            AcmeServerException exception = new AcmeServerException(ProblemType.SERVER_INTERNAL, "Failed to get resource: "+request.getURI());
            exception.addSuppressed(e);
            exception.printStackTrace();
            throw exception;
        }finally {
            request.completed();
            HttpClientUtils.closeQuietly(httpclient);
        }
    }
}
