package com.example.agent.model.football;

import com.fasterxml.jackson.databind.JsonNode;

public class Coach {
    private int id;
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String nationality;
    private Contract contract;

    public Coach(int id, String name, String nationality, String countryOfBirth, String role, String dateOfBirth) {
        this.id = id;
        if (name != null) {
            String[] parts = name.split(" ", 2);
            this.firstName = parts[0];
            this.lastName = parts.length > 1 ? parts[1] : "";
        } else {
            this.firstName = null;
            this.lastName = null;
        }
        this.nationality = nationality;
        this.dateOfBirth = dateOfBirth;
    }

    public static Coach fromJson(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;

        int id = 0;
        // try both numeric and string id representations
        if (node.hasNonNull("id")) {
            JsonNode idNode = node.get("id");
            if (idNode.isNumber()) id = idNode.intValue();
            else {
                String idText = idNode.asText(null);
                try { id = idText != null ? Integer.parseInt(idText) : 0; } catch (NumberFormatException ignored) {}
            }
        }

        String name = node.path("name").asText(null);
        String nationality = node.path("nationality").asText(null);
        String countryOfBirth = node.path("countryOfBirth").asText(null);
        String role = node.path("role").asText(null);
        String dateOfBirth = node.path("dateOfBirth").asText(null);

        Coach coach = new Coach(id, name, nationality, countryOfBirth, role, dateOfBirth);

        // parse contract if available as nested object
        JsonNode contractNode = node.path("contract");
        Contract contract = null;
        if (!contractNode.isMissingNode() && !contractNode.isNull()) {
            contract = Contract.fromJson(contractNode);
        }

        // fallback: contract fields may be present directly on the coach object
        if (contract == null) {
            String start = firstNonNullText(node, "contractStart", "contract_start", "contractFrom", "startDate", "signed", "dateFrom");
            String end = firstNonNullText(node, "contractEnd", "contract_end", "contractUntil", "until", "untilDate", "dateTo", "expires");
            if (start != null || end != null) {
                contract = new Contract(start, end);
            }
        }

        if (contract != null) {
            coach.setContract(contract);
        }

        return coach;
    }

    private static String firstNonNullText(JsonNode node, String... keys) {
        for (String k : keys) {
            if (node.has(k) && !node.get(k).isNull()) {
                String v = node.get(k).asText(null);
                if (v != null && !v.isEmpty()) return v;
            }
        }
        return null;
    }


    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
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
    public Contract getContract() {
        return contract;
    }
    public void setContract(Contract contract) {
        this.contract = contract;
    }

    @Override
    public String toString() {
        return "Coach{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", dateOfBirth='" + dateOfBirth + '\'' +
                ", nationality='" + nationality + '\'' +
                ", contract=" + contract +
                '}';
    }
}
