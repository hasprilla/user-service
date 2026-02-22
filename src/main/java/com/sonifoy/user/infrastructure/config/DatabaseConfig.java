package com.sonifoy.user.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.sonifoy.user.infrastructure.adapter.out.persistence")
public class DatabaseConfig {
}
