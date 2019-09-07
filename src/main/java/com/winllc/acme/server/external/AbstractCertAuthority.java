package com.winllc.acme.server.external;

import com.winllc.acme.common.CertificateAuthoritySettings;

public abstract class AbstractCertAuthority implements CertificateAuthority {

    protected String name;
    protected CertificateAuthoritySettings settings;


    protected AbstractCertAuthority(CertificateAuthoritySettings settings){
        this.settings = settings;
    }

    @Override
    public String getName() {
        return settings.getName();
    }
}
