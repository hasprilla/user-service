package com.sonifoy.user.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories;

@Configuration
@EnableReactiveCassandraRepositories(basePackages = "com.sonifoy.user.infrastructure.adapter.out.persistence")
public class CassandraConfig {
}
