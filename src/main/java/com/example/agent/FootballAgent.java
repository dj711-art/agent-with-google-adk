package com.example.agent;

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
import java.util.Map;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


public class FootballAgent {
    private static final String USER_ID = "footydebm@gmail.com";
    private static final String NAME = "football_agent";
    private static final Logger LOGGER = Logger.getLogger(FootballAgent.class.getName());
    private static final SimpleRateLimiter RATE_LIMITER = new SimpleRateLimiter(10, 60_000L);



    public static final BaseAgent ROOT_AGENT = initAgent();
    public static final String BASE_URL = "https://api.football-data.org/v4";
    // Cache: key = competition code (e.g. "PL"), value = String[2] where [0]=results JSON, [1]=fixtures JSON
    private static final ConcurrentHashMap<String, String[]> COMP_CACHE = new ConcurrentHashMap<>();
    // Key team name value id
    private static final ConcurrentHashMap<String, String> TEAM_IDs_CACHE = new ConcurrentHashMap<>();
    // Add this field near other statics in FootballAgent
    private static final ConcurrentHashMap<String, String> URL_CACHE = new ConcurrentHashMap<>();
    static {
        getPlTeamIds();
    }

    public static BaseAgent initAgent() {
        return LlmAgent.builder()
                .name(NAME)
                .model("gemini-2.0-flash")
                .description("Agent that provides latest football match results, upcoming fixtures and simple match analysis.")
                .instruction("You are a helpful football assistant. Use available tools to return results, fixtures and analysis.")
                .tools(
                        FunctionTool.create(FootballAgent.class,"getPlTeamIds"),
                        FunctionTool.create(FootballAgent.class, "getEplDetails"),
                        FunctionTool.create(FootballAgent.class, "getLatestResults"),
                        FunctionTool.create(FootballAgent.class, "getFixtures"),
                        FunctionTool.create(FootballAgent.class, "analyzeMatch"))
                .build();


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
            @Schema(name = "competition", description = "Competition code or ID (e.g. PL, CL, BL1)")
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
            for (JsonNode t : teams) {
                JsonNode idNode = t.path("id");
                JsonNode nameNode = t.path("name");
                TEAM_IDs_CACHE.put(nameNode.asText(),idNode.asText());
                if (!idNode.isMissingNode() && !nameNode.isMissingNode()) {
                    sj.add(String.format("%s: %s", idNode.asText(), nameNode.asText()));
                }
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

    // Helper: simple HTTP GET with X-Auth-Token header. Returns response body or null on failure.
    private static String fetchUrlWithApiKey(String url, String apiKey) {
        LOGGER.info("Fetching URL: " + url);
        // Enforce rate limit (10 requests per 60 seconds)
        // Enforce rate limit (10 requests per 60 seconds)
        if (!RATE_LIMITER.tryAcquire()) {
            LOGGER.warning("Rate limit exceeded for fetchUrlWithApiKey — attempting to return cached response if available.");
            String cached = URL_CACHE.get(url);
            if (cached != null) {
                LOGGER.info("Returning cached response for URL: " + url);
                return cached;
            }
            LOGGER.warning("No cached response available for URL: " + url);
            return null;
        }
        try (HttpClient client = HttpClient.newHttpClient()){
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Auth-Token", apiKey)
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            LOGGER.info(String.format("Football Agent status code: %s response: %s" , resp.statusCode(),resp.body()));
            if (resp.statusCode() / 100 == 2) {
                String body = resp.body();
                // update URL cache on successful fetch
                URL_CACHE.put(url, body);
                return body;
            } else {
                // non-2xx: try to return cached if present
                String cached = URL_CACHE.get(url);
                if (cached != null) {
                    LOGGER.info("Non-2xx response — returning cached response for URL: " + url);
                    return cached;
                }
                return null;
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            String cached = URL_CACHE.get(url);
            if (cached != null) {
                LOGGER.info("Interrupted — returning cached response for URL: " + url);
                return cached;
            }
            LOGGER.log(Level.SEVERE, "Football Agent fetchUrlWithApiKey interrupted: " + ie.getMessage());
            return null;
        } catch (IOException ioe) {
            String cached = URL_CACHE.get(url);
            if (cached != null) {
                LOGGER.info("IO error — returning cached response for URL: " + url);
                return cached;
            }
            LOGGER.log(Level.SEVERE, "Football Agent fetchUrlWithApiKey IO error: " + ioe.getMessage());
            return null;
        }
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
