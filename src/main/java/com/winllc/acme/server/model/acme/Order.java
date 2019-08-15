package com.winllc.acme.server.model.acme;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

//RFC8555 Section 7.1.3
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Order extends ExpiresObject {
    //required
    private Identifier[] identifiers;
    //optional
    private String notBefore;
    //optional
    private String notAfter;
    //optional
    private ProblemDetails error;
    //required
    private String[] authorizations;
    //required
    private String finalize;
    //optional
    private String certificate;

    public Identifier[] getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Identifier[] identifiers) {
        this.identifiers = identifiers;
    }

    public String getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(String notBefore) {
        this.notBefore = notBefore;
    }

    public String getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(String notAfter) {
        this.notAfter = notAfter;
    }

    public ProblemDetails getError() {
        return error;
    }

    public void setError(ProblemDetails error) {
        this.error = error;
    }

    public String[] getAuthorizations() {
        return authorizations;
    }

    public void setAuthorizations(String[] authorizations) {
        this.authorizations = authorizations;
    }

    public String getFinalize() {
        return finalize;
    }

    public void setFinalize(String finalize) {
        this.finalize = finalize;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    @Override
    public boolean isValid() {
        return identifiers != null && StringUtils.isNotBlank(finalize);
    }

    @Override
    public String toString() {
        return "Order{" +
                "identifiers=" + Arrays.toString(identifiers) +
                ", notBefore='" + notBefore + '\'' +
                ", notAfter='" + notAfter + '\'' +
                ", error=" + error +
                ", authorizations=" + Arrays.toString(authorizations) +
                ", finalize='" + finalize + '\'' +
                ", certificate='" + certificate + '\'' +
                ", expires='" + expires + '\'' +
                ", status='" + status + '\'' +
                ", resource='" + resource + '\'' +
                '}';
    }
}
