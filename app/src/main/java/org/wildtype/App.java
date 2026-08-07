package org.wildtype;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.wildtype.config.WildtypeConfig;

@SpringBootApplication
@Import(WildtypeConfig.class)
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
