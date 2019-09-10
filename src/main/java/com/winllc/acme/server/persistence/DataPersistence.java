package com.winllc.acme.server.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.NoRepositoryBean;


@NoRepositoryBean
public interface DataPersistence<T> extends MongoRepository<T, String> {

}
