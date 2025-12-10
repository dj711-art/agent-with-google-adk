package com.example.agent;

import com.example.agent.model.football.Team;
import com.example.agent.util.SimpleRateLimiter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.example.agent.util.FootballAPIRequestHandler.BASE_URL;
import static com.example.agent.util.FootballAPIRequestHandler.fetchUrlWithApiKey;


public class FootballAgent {
    private static final String USER_ID = "footydebm@gmail.com";
    private static final String NAME = "football_agent";
    private static final Logger LOGGER = Logger.getLogger(FootballAgent.class.getName());




    public static final BaseAgent ROOT_AGENT = initAgent();

    // Cache: key = competition code (e.g. "PL"), value = String[2] where [0]=results JSON, [1]=fixtures JSON
    private static final ConcurrentHashMap<String, String[]> COMP_CACHE = new ConcurrentHashMap<>();
    // Key team name value id
    private static final ConcurrentHashMap<String, String> TEAM_IDs_CACHE = new ConcurrentHashMap<>();

    //
    private static List<Team> teamList = new ArrayList<>();
    static {
        getPlTeamIds();
    }

    public static BaseAgent initAgent() {
        return LlmAgent.builder()
                .name(NAME)
                .model("gemini-2.5-flash")
                .description("Agent that provides latest football match results, upcoming fixtures and simple match analysis.")
                .instruction("You are a helpful football assistant. Use available tools to return results, fixtures, team squad and analysis.")
                .tools(
                        FunctionTool.create(FootballAgent.class,"getPlTeamIds"),
                        FunctionTool.create(FootballAgent.class, "getEplDetails"),
                        FunctionTool.create(FootballAgent.class, "getLatestResults"),
                        FunctionTool.create(FootballAgent.class, "getFixtures"),
                        FunctionTool.create(FootballAgent.class, "analyzeMatch"),
                        FunctionTool.create(FootballAgent.class, "getTeamSquad"))
                .build();


    }

    @Schema(name = "getTeamSquad", description = "Returns squad for a team name or ID (uses FOOTBALL_API_KEY).")
    public static Map<String, String> getTeamSquad(
            @Schema(name = "team", description = "Team name or ID (e.g. Liverpool or 64)") String team) {
        String apiKey = System.getenv("FOOTBALL_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return Map.of("status", "success",
                    "report", "No API key configured. Example: Squad: Player A, Player B, Player C (sample).");
        }

        if (team == null || team.isBlank()) {
            return Map.of("status", "error", "report", "No team provided.");
        }

        String original = team.trim();
        String teamId = null;

        // direct cache lookup
        if (TEAM_IDs_CACHE.containsKey(original)) {
            teamId = TEAM_IDs_CACHE.get(original);
        } else {
            // try append " FC" for short names (keep same heuristic as getFixtures)
            if (!original.endsWith("FC")) {
                String t2 = original + " FC";
                if (TEAM_IDs_CACHE.containsKey(t2)) {
                    teamId = TEAM_IDs_CACHE.get(t2);
                }
            }
            // case-insensitive and partial match in cache
            if (teamId == null) {
                for (Map.Entry<String, String> e : TEAM_IDs_CACHE.entrySet()) {
                    String key = e.getKey();
                    if (key.equalsIgnoreCase(original)
                            || key.toLowerCase().contains(original.toLowerCase())
                            || original.toLowerCase().contains(key.toLowerCase())) {
                        teamId = e.getValue();
                        break;
                    }
                }
            }
        }

        // numeric string provided -> treat as ID
        if (teamId == null && original.matches("\\d+")) {
            teamId = original;
        }

        // fallback: check loaded teamList (Team.fromJson entries)
        if (teamId == null && teamList != null) {
            for (Team t : teamList) {
                try {
                    String name = t.getName();
                    String id = String.valueOf(t.getId());
                    if (name != null && (name.equalsIgnoreCase(original)
                            || name.toLowerCase().contains(original.toLowerCase())
                            || original.toLowerCase().contains(name.toLowerCase()))) {
                        teamId = id;
                        LOGGER.info("=== Found squad for team " + name + " (id=" + id + ")");
                        return Map.of("status", "success", "report", t.getSquad().toString());
                    }
                    if (original.equals(id)) {
                        teamId = id;
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (teamId == null) {
            return Map.of("status", "error", "report",
                    "Team not found: " + original + ". Try providing the numeric team ID or a more specific name.");
        }

        String url = String.format("%s/teams/%s", BASE_URL, teamId);
        String json = fetchUrlWithApiKey(url, apiKey);
        if (json == null) {
            return Map.of("status", "error", "report",
                    "Failed to fetch squad for " + original + " (id=" + teamId + ").");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode squad = root.path("squad");
            if (squad.isMissingNode() || squad.isEmpty()) {
                return Map.of("status", "success", "report", json);
            } else {
                return Map.of("status", "success", "report", squad.toString());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error parsing squad JSON: " + e.getMessage());
            return Map.of("status", "success", "report", json);
        }
    }

    @Schema(name = "getEplDetails", description = "Returns English Premier League (EPL) competition details. Uses FOOTBALL_API_KEY when configured.")
    public static Map<String, String> getEplDetails() {
        String apiKey = System.getenv("FOOTBALL_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return Map.of(
                    "status", "success",
                    "report", "No API key configured. Example: Premier League competition details (sample).");
        }

        String url = String.format("%s/competitions/PL", BASE_URL);
        String json = fetchUrlWithApiKey(url, apiKey);

        if (json == null) {
            return Map.of("status", "error", "report", "Failed to fetch EPL details.");
        }
        return Map.of("status", "success", "report", json);
    }

    @Schema(name = "getYesterdaysResult", description = "No-argument tool that returns yesterday's finished matches (uses FOOTBALL_API_KEY when configured).")
    public static Map<String, String> getYesterdaysResult() {
        String apiKey = System.getenv("FOOTBALL_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return Map.of(
                    "status", "success",
                    "report", "No API key configured. Example: Manchester United 2-1 Liverpool (sample).");
        }
        String url = String.format("%s/matches?date=YESTERDAY",
                BASE_URL);
        String json = fetchUrlWithApiKey(url, apiKey);
        if (json == null) {
            return Map.of("status", "error", "report", "Failed to fetch results for yesterday.");
        }
        return Map.of("status", "success", "report", json);
    }

        // Returns a short report (raw JSON if API used) about latest finished matches for a competition.
    public static Map<String, String> getLatestResults(
            @Schema(name = "competition", description = "Competition code or ID (e.g. WC, CL, BL1, DED, PL, CL, BL1)")
            String competition) {
        String apiKey = System.getenv("FOOTBALL_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            // Stubbed sample response
            return Map.of(
                    "status", "success",
                    "report", "No API key configured. Example: Manchester United 2-1 Liverpool (sample data).");
        }

        String url = String.format("%s/competitions/%s/matches?status=FINISHED", BASE_URL,competition);
        String json = fetchUrlWithApiKey(url, apiKey);
        LOGGER.info(String.format("Football Agent getLatestResults: %s" , json));
        if (json == null) {
            if (COMP_CACHE.containsKey(competition)){
                String cachedJson = COMP_CACHE.get(competition)[0];
                if (cachedJson != null){
                    return Map.of("status", "success", "report", cachedJson + "\n\n(Note: This is cached data due to API fetch failure.)");
                }
            }
            return Map.of("status", "error", "report", "Failed to fetch results for " + competition + ".");
        }
        // Update cache: set index 1 to fixtures
        COMP_CACHE.compute(competition, (k, v) -> {
            String[] arr = (v == null) ? new String[2] : v;
            arr[0] = json;
            return arr;
        });
        // For a minimal scaffold, return raw JSON as the report. Production: parse JSON and format.
        return Map.of("status", "success", "report", json);
    }

    @Schema(name = "getPlTeamIds", description = "Returns PL team IDs and names (uses FOOTBALL_API_KEY).")
    public static void getPlTeamIds() {
        String apiKey = System.getenv("FOOTBALL_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            // example stub
            String sample = "64: Liverpool\n65: Manchester City\n66: Chelsea";
            return;
        }

        String url = String.format("%s/competitions/PL/teams", BASE_URL);
        String json = fetchUrlWithApiKey(url, apiKey);
        if (json == null) {
            return;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode teams = root.path("teams");
            if (!teams.isArray() || teams.isEmpty()) {
                return;
            }

            StringJoiner sj = new StringJoiner("\n");


            for (JsonNode teamJsonNode : teams) {
                // Parse each team node into Team object

                JsonNode idNode = teamJsonNode.path("id");
                JsonNode nameNode = teamJsonNode.path("name");
                TEAM_IDs_CACHE.put(nameNode.asText(),idNode.asText());
                if (!idNode.isMissingNode() && !nameNode.isMissingNode()) {
                    sj.add(String.format("%s: %s", idNode.asText(), nameNode.asText()));
                }


                teamList.add(Team.fromJson(teamJsonNode));
            }
            LOGGER.info(TEAM_IDs_CACHE.toString());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing PL teams JSON: " + e.getMessage());
        }
    }

    // Returns upcoming fixtures for a team (accepts team ID or short name depending on API).
    public static Map<String, String> getFixtures(
            @Schema(name = "team", description = "Team ID or short name (depends on API provider)")
            String team) {
        String apiKey = System.getenv("FOOTBALL_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return Map.of("status", "success", "report", "No API key configured. Example fixture: Team A vs Team B on 2025-12-05.");
        }

        // Example endpoint — adapt to chosen API. If team is an ID, use it directly; otherwise implement lookup.
        LOGGER.info("Passed TEAM : " + team);
        if (!team.endsWith("FC")){
            team = team + " FC";
        }
        if (TEAM_IDs_CACHE.containsKey(team)){
            team = TEAM_IDs_CACHE.get(team);
        }
        LOGGER.info("RESOLVED TEAM ID: " + team);
        String url = String.format("%s/teams/%s/matches?status=SCHEDULED", BASE_URL,team);
        String json = fetchUrlWithApiKey(url, apiKey);
        if (json == null) {
            return Map.of("status", "error", "report", "Failed to fetch fixtures for " + team + ".");
        }

        return Map.of("status", "success", "report", json);
    }

    // Returns simple analysis for a match (accepts match ID). This scaffolds a basic analysis string.
    public static Map<String, String> analyzeMatch(
            @Schema(name = "matchId", description = "Match ID from the data provider")
            String matchId) {
        String apiKey = System.getenv("FOOTBALL_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return Map.of("status", "success", "report", "No API key configured. Sample analysis: Home side dominated possession and converted chances.");
        }

        String url = String.format("%s/matches/%s", BASE_URL,matchId);
        String json = fetchUrlWithApiKey(url, apiKey);
        if (json == null) {
            return Map.of("status", "error", "report", "Failed to fetch match details for " + matchId + ".");
        }

        // Minimal analysis stub: in production parse JSON (events, possession, shots) and create natural language analysis.
        String analysis = "Raw match data: " + json;
        return Map.of("status", "success", "report", analysis);
    }


    static void main(String[] args) {
        InMemoryRunner runner = new InMemoryRunner(ROOT_AGENT);
        Session session = runner.sessionService().createSession(NAME, USER_ID).blockingGet();

        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            while (true) {
                System.out.print("\nYou > ");
                String userInput = scanner.nextLine();
                if ("quit".equalsIgnoreCase(userInput)) break;
                Content userMsg = Content.fromParts(Part.fromText(userInput));
                Flowable<Event> events = runner.runAsync(USER_ID, session.id(), userMsg);
                System.out.print("\nAgent > ");
                events.blockingForEach(event -> System.out.println(event.stringifyContent()));
            }
        }
    }
}
