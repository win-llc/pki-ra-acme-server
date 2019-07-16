package com.winllc.acme.server.challenge;

import com.winllc.acme.server.model.data.ChallengeData;

public interface ChallengeVerification {

    void verify(ChallengeData challenge);
}
