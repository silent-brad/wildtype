package org.wildtype.db;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.wildtype.Sighting;

@Repository
public class SightingRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public SightingRepository(DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    public long insert(Sighting s) {
        var params = new MapSqlParameterSource()
                .addValue("detectedAt", s.detectedAt().toString())
                .addValue("species", s.species())
                .addValue("confidence", s.confidence())
                .addValue("imagePath", s.imagePath());

        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(
                "INSERT INTO sightings (detected_at, species, confidence, image_path) "
                        + "VALUES (:detectedAt, :species, :confidence, :imagePath)",
                params,
                keyHolder);
        return keyHolder.getKey().longValue();
    }

    public List<Sighting> findRecent(int limit) {
        return jdbc.query(
                "SELECT id, detected_at, species, confidence, image_path FROM sightings "
                        + "ORDER BY detected_at DESC LIMIT :limit",
                Map.of("limit", limit),
                (rs, rowNum) -> new Sighting(
                        rs.getLong("id"),
                        Instant.parse(rs.getString("detected_at")),
                        rs.getString("species"),
                        rs.getFloat("confidence"),
                        rs.getString("image_path")));
    }

    public Map<String, Integer> countBySpecies() {
        return jdbc
                .query(
                        "SELECT species, COUNT(*) AS cnt FROM sightings GROUP BY species",
                        (rs, rowNum) -> Map.entry(rs.getString("species"), rs.getInt("cnt")))
                .stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Optional<Sighting> findById(long id) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    "SELECT id, detected_at, species, confidence, image_path FROM sightings WHERE id = :id",
                    Map.of("id", id),
                    (rs, rowNum) -> new Sighting(
                            rs.getLong("id"),
                            Instant.parse(rs.getString("detected_at")),
                            rs.getString("species"),
                            rs.getFloat("confidence"),
                            rs.getString("image_path"))));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
