package com.winllc.acme.server.process;

import com.winllc.acme.server.model.data.DataObject;
import com.winllc.acme.server.model.data.DirectoryData;

public interface AcmeDataProcessor<T extends DataObject> {
    T buildNew(DirectoryData directoryData);

}
