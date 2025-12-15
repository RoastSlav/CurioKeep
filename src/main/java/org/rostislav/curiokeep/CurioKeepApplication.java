package org.rostislav.curiokeep;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CurioKeepApplication {

    public static void main(String[] args) {
        SpringApplication.run(CurioKeepApplication.class, args);
    }

}
