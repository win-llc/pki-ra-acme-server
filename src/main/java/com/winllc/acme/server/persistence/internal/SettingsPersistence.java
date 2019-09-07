package com.winllc.acme.server.persistence.internal;

import com.winllc.acme.common.Settings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface SettingsPersistence<T extends Settings> extends MongoRepository<T, String> {
    T findByName(String name);
    List<T> findAll();
}
