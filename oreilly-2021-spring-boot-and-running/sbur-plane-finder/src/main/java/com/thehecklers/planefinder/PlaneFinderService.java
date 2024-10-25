package com.thehecklers.planefinder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlaneFinderService {
    private final PlaneRepository repo;
    private URL acURL;
    private final ObjectMapper om;

    @SneakyThrows
    public PlaneFinderService(PlaneRepository repo) {
        this.repo = repo;

//        acURL = new URL("http://192.168.1.193/ajax/aircraft");
        acURL = new URL("http://example.com");
        om = new ObjectMapper();
    }

    public Iterable<Aircraft> getAircraft() throws IOException {
        List<Aircraft> positions = new ArrayList<>();

        JsonNode aircraftNodes = null;
        try {
            aircraftNodes = om.readTree(acURL)
                    .get("aircraft");

            aircraftNodes.iterator().forEachRemaining(node -> {
                try {
                    positions.add(om.treeToValue(node, Aircraft.class));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("\n>>> IO Exception: " + e.getLocalizedMessage() +
                    ", generating and providing sample data.\n");
            return saveSamplePositions();
        }

        if (positions.size() > 0) {
            positions.forEach(System.out::println);

            repo.deleteAll();
            return repo.saveAll(positions);
        } else {
            System.out.println("\n>>> No positions to report, generating and providing sample data.\n");
            return saveSamplePositions();
        }
    }

    private Iterable<Aircraft> saveSamplePositions() {
        repo.deleteAll();

        // Spring Airlines flight 001 en route, flying STL to SFO, at 30000' currently over Kansas City
        var ac1 = new Aircraft("SAL001", "N12345", "SAL001", "LJ",
                30000, 280, 440,
                39.2979849, -94.71921);

        // Spring Airlines flight 002 en route, flying SFO to STL, at 40000' currently over Denver
        var ac2 = new Aircraft("SAL002", "N54321", "SAL002", "LJ",
                40000, 65, 440,
                39.8560963, -104.6759263);

        // Spring Airlines flight 002 en route, flying SFO to STL, at 40000' currently just past DEN
        var ac3 = new Aircraft("SAL003", "N54321", "SAL002", "LJ",
                41000, 64, 430,
                39.8412964, -105.0048267);

        var ac4 = new Aircraft("SAL004", "", "SAL003", "LJ",
                40000, 70, 150,
                39.8412965, -106.0048267);

        return repo.saveAll(List.of(ac1, ac2, ac3, ac4));
    }
}

