package org.rostislav.curiokeep;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:curiokeep_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.flyway.enabled=false",
    "spring.sql.init.mode=never"
})
@Import(NoopModuleConfig.class)
class CurioKeepApplicationTests {

    @Test
    void contextLoads() {
    }

}
