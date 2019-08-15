package com.winllc.acme.server.model.data;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.acme.Challenge;
import com.winllc.acme.server.model.acme.Directory;

public class ChallengeData extends DataObject<Challenge> {

    private String authorizationId;

    public ChallengeData(Challenge obj, DirectoryData directoryData) {
        super(obj, directoryData);
    }

    @Override
    public String buildUrl() {
        return buildBaseUrl() + "chall/" + getId();
    }

    public String getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(String authorizationId) {
        this.authorizationId = authorizationId;
    }

    @Override
    public String toString() {
        return "ChallengeData{" +
                "authorizationId='" + authorizationId + '\'' +
                "} " + super.toString();
    }
}
