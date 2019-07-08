package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.data.DirectoryData;

import java.util.Optional;

public class DirectoryPersistence implements DataPersistence<DirectoryData> {
    @Override
    public Optional<DirectoryData> getById(String id) {
        //TODO
        return Optional.empty();
    }

    @Override
    public DirectoryData save(DirectoryData data) {
        //TODO
        return null;
    }
}
