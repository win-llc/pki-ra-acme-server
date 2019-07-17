package com.winllc.acme.server.model.requestresponse;

import com.winllc.acme.server.model.acme.Identifier;
import org.apache.commons.lang3.ArrayUtils;

/*
Section 7.4

The body of the POST is a JWS object whose JSON payload is a subset of the order object defined in Section 7.1.3, containing the fields that describe the certificate to be issued:

identifiers (required, array of object):
An array of identifier objects that the client wishes to submit an order for.
type (required, string):
The type of identifier.
value (required, string):
The identifier itself.
notBefore (optional, string):
The requested value of the notBefore field in the certificate, in the date format defined in [RFC3339].
notAfter (optional, string):
The requested value of the notAfter field in the certificate, in the date format defined in [RFC3339].
 */

public class OrderRequest {
    //required
    private Identifier[] identifiers;
    //optional
    private String notBefore;
    //optional
    private String notAfter;

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

    public boolean isValid(){
        if(ArrayUtils.isEmpty(identifiers)) return false;

        return true;
    }
}
