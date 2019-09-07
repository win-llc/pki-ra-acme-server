package com.winllc.acme.server.persistence.internal;

import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.common.DirectoryDataSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CertificateAuthoritySettingsPersistence extends SettingsPersistence<CertificateAuthoritySettings> {
}
