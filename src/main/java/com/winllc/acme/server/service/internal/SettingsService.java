package com.winllc.acme.server.service.internal;

import java.util.List;

public interface SettingsService<T, R> {
    T save(T settings) throws Exception;
    T findSettingsByName(String name);
    T findSettingsById(String id) throws Exception;
    R findByName(String name) throws Exception;
    void delete(String name);
    List<T> findAllSettings();
    List<R> findAll();
    R load(T settings) throws Exception;
}
