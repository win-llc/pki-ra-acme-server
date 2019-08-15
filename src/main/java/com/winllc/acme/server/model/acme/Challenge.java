package com.winllc.acme.server.model.acme;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;

//RFC8555 Section 8
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Challenge extends BaseAcmeObject {
    //required
    private String type;
    //required
    private String url;
    //optional
    private String validated;
    //required
    private String token;
    //optional
    private ProblemDetails error;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getValidated() {
        return validated;
    }

    public void setValidated(String validated) {
        this.validated = validated;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public ProblemDetails getError() {
        return error;
    }

    public void setError(ProblemDetails error) {
        this.error = error;
    }

    @Override
    public boolean isValid() {
        return StringUtils.isNotBlank(type) && StringUtils.isNotBlank(url) && StringUtils.isNotBlank(token);
    }

    @Override
    public String toString() {
        return "Challenge{" +
                "type='" + type + '\'' +
                ", url='" + url + '\'' +
                ", validated='" + validated + '\'' +
                ", error=" + error +
                ", status='" + status + '\'' +
                ", resource='" + resource + '\'' +
                '}';
    }
}
