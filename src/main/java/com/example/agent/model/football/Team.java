package com.example.agent.model.football;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class Team {
    private int id;
    private String name;
    private String shortName;
    private String tla;
    private String crestUrl;
    private Coach coach;
    private String address;
    private String website;
    private String founded;
    private String clubColors;
    private String venue;
    private List<Player> squad;
    private List<Competition> runningCompetitions;

    public List<Competition> getRunningCompetitions() {
        return runningCompetitions;
    }

    public void setRunningCompetitions(List<Competition> runningCompetitions) {
        this.runningCompetitions = runningCompetitions;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public List<Player> getSquad() {
        return squad;
    }

    public void setSquad(List<Player> squad) {
        this.squad = squad;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getTla() {
        return tla;
    }

    public void setTla(String tla) {
        this.tla = tla;
    }

    public String getCrestUrl() {
        return crestUrl;
    }

    public void setCrestUrl(String crestUrl) {
        this.crestUrl = crestUrl;
    }

    public Coach getCoach() {
        return coach;
    }

    public void setCoach(Coach coach) {
        this.coach = coach;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getFounded() {
        return founded;
    }

    public void setFounded(String founded) {
        this.founded = founded;
    }

    public String getClubColors() {
        return clubColors;
    }

    public void setClubColors(String clubColors) {
        this.clubColors = clubColors;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public Team(int id, String name, String shortName, String tla, String crestUrl) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.tla = tla;
        this.crestUrl = crestUrl;
    }

    public static Team fromJson(JsonNode node) {
        if (node == null || node.isMissingNode()) return null;
        String id = node.path("id").asText(null);
        String name = node.path("name").asText(null);
        String shortName = node.path("shortName").asText(null);
        String tla = node.path("tla").asText(null);
        String crestUrl = node.path("crest").asText(null); // API may use "crest" or "crestUrl"
        if (crestUrl == null) crestUrl = node.path("crestUrl").asText(null);
        Team team = new Team(Integer.parseInt(id), name, shortName, tla, crestUrl);
        // parse coach if available
        JsonNode coachNode = node.path("coach");
        if (!coachNode.isMissingNode() && !coachNode.isNull()) {
            Coach coach = Coach.fromJson(coachNode);
            team.setCoach(coach);
        }
        // parse address, website, founded, clubColors, venue
        team.setAddress(node.path("address").asText(null));
        team.setWebsite(node.path("website").asText(null));
        team.setFounded(node.path("founded").asText(null));
        team.setClubColors(node.path("clubColors").asText(null));
        team.setVenue(node.path("venue").asText(null));

        // parse squad / players
        JsonNode squadNode = node.path("squad");
        if (squadNode.isMissingNode() || squadNode.isNull()) {
            squadNode = node.path("players");
        }
        if (!squadNode.isMissingNode() && squadNode.isArray()) {
            List<Player> players = new ArrayList<>();
            for (JsonNode p : squadNode) {
                Player player = Player.fromJson(p);
                if (player != null) players.add(player);
            }
            team.setSquad(players);
        }

        JsonNode compNode = node.path("runningCompetitions");
        if (compNode.isMissingNode() || compNode.isNull()) compNode = node.path("runningCompetitions");
        if (compNode.isMissingNode() || compNode.isNull()) compNode = node.path("competition");
        if (compNode.isMissingNode() || compNode.isNull()) compNode = node.path("competitions");

        if (!compNode.isMissingNode() && !compNode.isNull()) {
            List<Competition> comps = new ArrayList<>();
            if (compNode.isArray()) {
                for (JsonNode c : compNode) {
                    Competition competition = Competition.fromJson(c);
                    if (competition != null) comps.add(competition);
                }
            } else if (compNode.isObject()) {
                Competition competition = Competition.fromJson(compNode);
                if (competition != null) comps.add(competition);
            }
            if (!comps.isEmpty()) team.setRunningCompetitions(comps);
        }
        return team;
    }

    @Override
    public String toString() {
        return "Team{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", shortName='" + shortName + '\'' +
                ", tla='" + tla + '\'' +
                ", crestUrl='" + crestUrl + '\'' +
                ", coach=" + coach +
                ", address='" + address + '\'' +
                ", website='" + website + '\'' +
                ", founded='" + founded + '\'' +
                ", clubColors='" + clubColors + '\'' +
                ", venue='" + venue + '\'' +
                ", squad=" + squad +
                ", runningCompetitions=" + runningCompetitions +
                '}';
    }
}
