package com.winllc.acme.server.model;

//RFC8555 Section 8
public class Challenge extends BaseAcmeObject {
    //required
    private String type;
    //required
    private String url;
    //optional
    private String validated;
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

    public ProblemDetails getError() {
        return error;
    }

    public void setError(ProblemDetails error) {
        this.error = error;
    }
}
