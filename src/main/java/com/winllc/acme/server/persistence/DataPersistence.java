package com.winllc.acme.server.persistence;

import java.util.Optional;

public interface DataPersistence<T> {
    Optional<T> getById(String id);
    T save(T data);
}
