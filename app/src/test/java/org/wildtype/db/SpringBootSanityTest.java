package org.wildtype.db;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.wildtype.config.WildtypeConfig;

@SpringBootTest
@ContextConfiguration(classes = {SpringBootSanityTest.TestDataSourceConfig.class, WildtypeConfig.class})
class SpringBootSanityTest {

    @Configuration
    static class TestDataSourceConfig {
        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:sqlite::memory:");
        }
    }

    @Test
    void contextLoads() {}
}
