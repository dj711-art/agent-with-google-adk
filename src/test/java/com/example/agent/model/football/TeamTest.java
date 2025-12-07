package com.example.agent.model.football;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TeamTest {

    @Test
    void readResponseJsonAndPopulateTeams() throws Exception {
        Path p = Path.of(".", "src","test", "resources", "eplTeams", "fictional.response.json");
        assertTrue(Files.exists(p), "test resource not found: " + p + " (user.dir=" + System.getProperty("user.dir") + ")");

        String json = Files.readString(p);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        JsonNode teamsNode = root.path("teams");
        if (teamsNode.isMissingNode()) {
            if (root.isArray()) {
                teamsNode = root;
            } else {
                fail("No 'teams' array found in " + p);
            }
        }

        List<Team> teams = new ArrayList<>();
        for (JsonNode node : teamsNode) {
            Team team = Team.fromJson(node);
            assertNotNull(team, "Team.fromJson returned null for node: " + node);
            assertTrue(team.getId() > 0, "Team id should be > 0");
            assertNotNull(team.getName(), "Team name should not be null");
            teams.add(team);
        }

        System.out.println("Parsed " + teams.size() + " teams  " + teams.toString());
        assertFalse(teams.isEmpty(), "No teams were parsed from " + p);
    }
}