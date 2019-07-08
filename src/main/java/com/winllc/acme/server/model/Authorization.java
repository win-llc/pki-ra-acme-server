package com.winllc.acme.server.model;

import java.util.Arrays;
import java.util.List;

//RFC8555 Section 7.1.4
public class Authorization extends BaseAcmeObject {
    //required
    private Identifier identifier;
    //optional
    private String expires;
    //required
    private Challenge[] challenges;
    //optional
    private Boolean wildcard;

    public Identifier getIdentifier() {
        return identifier;
    }

    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
    }


    public String getExpires() {
        return expires;
    }

    public void setExpires(String expires) {
        this.expires = expires;
    }

    public Challenge[] getChallenges() {
        return challenges;
    }

    public void setChallenges(Challenge[] challenges) {
        this.challenges = challenges;
    }

    public void addChallenge(Challenge challenge){
        if(challenges == null) challenges = new Challenge[0];

        List<Challenge> list = Arrays.asList(challenges);
        list.add(challenge);
        challenges = list.toArray(new Challenge[0]);
    }

    public Boolean getWildcard() {
        return wildcard;
    }

    public void setWildcard(Boolean wildcard) {
        this.wildcard = wildcard;
    }
}
