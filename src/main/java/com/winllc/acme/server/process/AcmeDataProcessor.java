package com.winllc.acme.server.process;

import com.winllc.acme.common.model.data.DataObject;
import com.winllc.acme.common.model.data.DirectoryData;

public interface AcmeDataProcessor<T extends DataObject> {
    T buildNew(DirectoryData directoryData);

}
