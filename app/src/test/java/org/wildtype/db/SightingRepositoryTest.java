package org.wildtype.db;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.wildtype.Sighting;

class SightingRepositoryTest {

    private Connection connection;
    private SingleConnectionDataSource ds;
    private SightingRepository repo;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        ds = new SingleConnectionDataSource(connection, false);
        var jdbc = new NamedParameterJdbcTemplate(ds);
        jdbc.getJdbcTemplate()
                .execute("CREATE TABLE sightings (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "detected_at TEXT NOT NULL, species TEXT NOT NULL, "
                        + "confidence REAL NOT NULL, image_path TEXT NOT NULL, downlink_format INTEGER)");
        repo = new SightingRepository(ds);
    }

    @AfterEach
    void tearDown() {
        ds.destroy();
    }

    @Test
    void insertAndFindRecent() {
        long id = repo.insert(new Sighting(0, Instant.now(), "Bird", 0.91f, "/captures/bird.jpg"));
        assertTrue(id > 0);

        List<Sighting> recent = repo.findRecent(10);
        assertEquals(1, recent.size());
        assertEquals("Bird", recent.get(0).species());
    }

    @Test
    void countBySpecies() {
        repo.insert(new Sighting(0, Instant.now(), "Bird", 0.91f, "/captures/bird1.jpg"));
        repo.insert(new Sighting(0, Instant.now(), "Bird", 0.85f, "/captures/bird2.jpg"));
        repo.insert(new Sighting(0, Instant.now(), "Cat", 0.72f, "/captures/cat.jpg"));

        Map<String, Integer> counts = repo.countBySpecies();
        assertEquals(2, counts.get("Bird"));
        assertEquals(1, counts.get("Cat"));
    }

    @Test
    void findById() {
        long id = repo.insert(new Sighting(0, Instant.now(), "Dog", 0.67f, "/captures/dog.jpg"));
        var found = repo.findById(id);
        assertTrue(found.isPresent());
        assertEquals("Dog", found.get().species());
        assertTrue(repo.findById(99999L).isEmpty());
    }
}
