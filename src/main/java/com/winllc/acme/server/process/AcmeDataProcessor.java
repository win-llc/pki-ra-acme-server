package com.winllc.acme.server.process;

import com.winllc.acme.server.model.data.DataObject;

public interface AcmeDataProcessor<T extends DataObject> {
    T buildNew();
}
