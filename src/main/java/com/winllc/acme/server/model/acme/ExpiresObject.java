package com.winllc.acme.server.model.acme;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class ExpiresObject<T extends BaseAcmeObject> extends BaseAcmeObject<T>{
    //TODO verify compliant with RFC3339
    private static DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;

    //optional
    protected String expires;

    public String getExpires() {
        return expires;
    }

    public void setExpires(String expires) {
        this.expires = expires;
    }

    public boolean isExpired(){
        boolean expired = false;
        if(StringUtils.isNotBlank(expires)){
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresOn = LocalDateTime.parse(expires, dtf);

            if(now.isAfter(expiresOn)){
                expired = true;
            }
        }

        return expired;
    }
}
