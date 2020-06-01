package com.winllc.acme.server.challenge;

import com.winllc.acme.common.model.data.ChallengeData;

public interface ChallengeVerification {

    void verify(ChallengeData challenge);
}
