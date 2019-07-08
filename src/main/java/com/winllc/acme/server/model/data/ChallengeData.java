package com.winllc.acme.server.model.data;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.Challenge;

public class ChallengeData extends DataObject<Challenge> {

    private String authorizationId;

    public ChallengeData(Challenge obj) {
        super(obj);
    }

    @Override
    public String buildUrl() {
        return Application.baseURL + "chall/" + getId();
    }

    public String getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(String authorizationId) {
        this.authorizationId = authorizationId;
    }
}
