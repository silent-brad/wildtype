package org.wildtype.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.wildtype.Sighting;
import org.wildtype.db.SightingRepository;

@Service
public class SightingService {

    private final SightingRepository repository;

    public SightingService(SightingRepository repository) {
        this.repository = repository;
    }

    public long record(Sighting sighting) {
        return repository.insert(sighting);
    }

    public List<Sighting> recent(int limit) {
        return repository.findRecent(limit);
    }

    public Map<String, Integer> counts() {
        return repository.countBySpecies();
    }
}
