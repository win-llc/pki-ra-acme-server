package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.data.CertData;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface CertificatePersistence extends DataPersistence<CertData> {

}
