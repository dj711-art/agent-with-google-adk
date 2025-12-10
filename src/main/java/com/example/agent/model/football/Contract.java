package com.example.agent.model.football;

import com.fasterxml.jackson.databind.JsonNode;

public class Contract {
    private String startDate;
    private String endDate;
    private boolean renewed;

    public Contract(String startDate, String endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public boolean isRenewed() {
        return renewed;
    }

    public void setRenewed(boolean renewed) {
        this.renewed = renewed;
    }

    public static Contract fromJson(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;

        // accept multiple common field names
        String start = firstNonNullText(node, "startDate", "start", "dateFrom", "signed");
        String end = firstNonNullText(node, "endDate", "until", "untilDate", "dateTo", "expires");

        if (start == null && end == null) return null;
        return new Contract(start, end);
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

    @Override
    public String toString() {
        return "Contract{" +
                "startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", renewed=" + renewed +
                '}';
    }
}
