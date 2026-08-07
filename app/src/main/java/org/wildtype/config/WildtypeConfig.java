package org.wildtype.config;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.wildtype.db.SightingRepository;
import org.wildtype.service.SightingService;
import org.wildtype.web.StreamController;

@Configuration
public class WildtypeConfig {

    @Bean
    SightingRepository sightingRepository(DataSource dataSource) {
        return new SightingRepository(dataSource);
    }

    @Bean
    SightingService sightingService(SightingRepository repo) {
        return new SightingService(repo);
    }

    @Bean
    StreamController streamController() {
        return new StreamController();
    }
}
