package com.example.agent.model.football;

import com.fasterxml.jackson.databind.JsonNode;

public class Competition {
    private final String status;
    private final String startDate;
    private final String endDate;
    private int id;
    private String name;
    private String code;
    private String areaName;

    public String getStatus() {
        return status;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public Competition(int id, String name, String status, String startDate, String endDate) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getEmblemUrl() {
        return emblemUrl;
    }

    public void setEmblemUrl(String emblemUrl) {
        this.emblemUrl = emblemUrl;
    }

    private String emblemUrl;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public static Competition fromJson(JsonNode node) {
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
        String status = node.path("status").asText(null);
        String type = node.path("type").asText(null);
        String emblemUrl = node.path("emblem").asText(null);
        String startDate = node.path("startDate").asText(null);
        if (startDate == null) startDate = node.path("start").asText(null);
        String endDate = node.path("endDate").asText(null);
        if (endDate == null) endDate = node.path("end").asText(null);

        Competition competition = new Competition(id, name, status, startDate, endDate);
        competition.setEmblemUrl(emblemUrl);
        return  competition;
    }

    public enum Status {
        PLANNED,
        REGISTRATION_OPEN,
        ONGOING,
        FINISHED,
        CANCELLED
    }

    @Override
    public String toString() {
        return "Competition{" +
                "status='" + status + '\'' +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", areaName='" + areaName + '\'' +
                ", emblemUrl='" + emblemUrl + '\'' +
                '}';
    }
}
