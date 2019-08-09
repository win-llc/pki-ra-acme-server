package com.winllc.acme.server.model.acme;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public abstract class ExpiresObject<T extends BaseAcmeObject> extends BaseAcmeObject<T>{
    //TODO verify compliant with RFC3339
    private static DateTimeFormatter dtf = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneId.of("UTC"));

    //optional
    protected String expires;

    public String getExpires() {
        return expires;
    }

    public void setExpires(String expires) {
        this.expires = expires;
    }

    public void willExpireInMinutes(int minutes){
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(minutes);
        expires = expiresAt.format(dtf);
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
