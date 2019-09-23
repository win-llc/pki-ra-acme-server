package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.data.DirectoryData;
import org.springframework.stereotype.Component;

import java.util.Optional;

public interface DirectoryPersistence extends DataPersistence<DirectoryData> {

}
