package com.UBM.BMW;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factor.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframrwork.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties ="toolkit.jobs.refresh-initial-delay=PT24H")
@Testcontainers
@SuppressWarnings({"rawtypes", "resource"})
public class BMWToolkitTests
{
    @Container
    @ServiceConnection
    static PostgreSQLContainer post = new PostgreSQLContainer("postgres:16");
    @Autowired
    JdbcClient jdbc;
    @Test
    void contectLoadsAndSchemaExists()
    {
        Integer tables = jdbc.sql("""
                select count(*) from information_schema.tables where table_schema = 'public'
                and table_name in ('chassis_ref','engine_applications','option_catalog','vehicles','vehicle_options',
                'listings','price_history','known_issues','seller_questions','listing_questions','recalls',
                'listing_scores','watchlists','alerts','dead_letter_jobs')
                """).query(Integer.class).single();
        assertThat(tables).isEqualTo(15);
    }
    @Test
    void seedDataIsLoaded()
    {
        assertThat(jdbc.sql("select count(*) from chassis_ref").query(Integer.class).single()).isGreaterThan(0);
        assertThat(jdbc.sql("select count(*) from known_issues").query(Integer.class).single()).isGreaterThan(0);
        assertThat(jdbc.sql("select count(*) from seller_questions").query(Integer.class).single()).isGreaterThan(0);
        assertThat(jdbc.sql("select count(*) from known_issues where cardinality(sources) = 0").query(Integer.class).single()).isGreaterThan(0);

    }
}