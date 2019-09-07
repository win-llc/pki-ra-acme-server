package com.winllc.acme.server.service.internal;

import java.util.List;

public interface SettingsService<T, R> {
    void save(T settings) throws Exception;
    T findSettingsByName(String name);
    R findByName(String name);
    void delete(String name);
    List<T> findAllSettings();
    List<R> findAll();
    void load(T settings) throws Exception;
}
