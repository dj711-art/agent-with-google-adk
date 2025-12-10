package com.example.agent.model.football;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

public class Player {
private int id;
    private String name;
    private String position;
    private String dateOfBirth;
    private String nationality;
    private String countryOfBirth;
    private Integer shirtNumber;
    private String role;

    public String getCountryOfBirth() {
        return countryOfBirth;
    }

    public void setCountryOfBirth(String countryOfBirth) {
        this.countryOfBirth = countryOfBirth;
    }

    public Integer getShirtNumber() {
        return shirtNumber;
    }

    public void setShirtNumber(Integer shirtNumber) {
        this.shirtNumber = shirtNumber;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Player(int id, String name, String position, String nationality, String countryOfBirth, String dateOfBirth, Integer shirtNumber, String role) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.nationality = nationality;
        this.countryOfBirth = countryOfBirth;
        this.dateOfBirth = dateOfBirth;
        this.shirtNumber = shirtNumber;
        this.role = role;
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

    public void setName(String name) {
        this.name = name;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public static Player fromJson(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;

        int id = 0;
        if (node.hasNonNull("id")) {
            JsonNode idNode = node.get("id");
            if (idNode.isNumber()) id = idNode.intValue();
            else {
                String idText = idNode.asText(null);
                try { id = idText != null ? Integer.parseInt(idText) : 0; } catch (NumberFormatException ignored) {}
            }
        }

        String name = node.path("name").asText(null);
        String position = node.path("position").asText(null);
        String nationality = node.path("nationality").asText(null);
        String countryOfBirth = node.path("countryOfBirth").asText(null);
        String dateOfBirth = node.path("dateOfBirth").asText(null);

        Integer shirtNumber = null;
        if (node.has("shirtNumber") && !node.get("shirtNumber").isNull()) {
            JsonNode sn = node.get("shirtNumber");
            if (sn.isNumber()) shirtNumber = sn.intValue();
            else {
                String snText = sn.asText(null);
                try { shirtNumber = snText != null ? Integer.valueOf(Integer.parseInt(snText)) : null; } catch (NumberFormatException ignored) {}
            }
        }

        String role = node.path("role").asText(null);

        return new Player(id, name, position, nationality, countryOfBirth, dateOfBirth, shirtNumber, role);
    }

    @Override
    public String toString() {
        return "Player{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", position='" + position + '\'' +
                ", dateOfBirth='" + dateOfBirth + '\'' +
                ", nationality='" + nationality + '\'' +
                ", countryOfBirth='" + countryOfBirth + '\'' +
                ", shirtNumber=" + shirtNumber +
                ", role='" + role + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Player player)) return false;
        return id == player.id && Objects.equals(name, player.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
