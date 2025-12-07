package com.example.agent.model.football;

import java.util.List;

public class Squad {
    private List<Player> players;

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    @Override
    public String toString() {
        return "Squad{" +
                "players=" + players +
                '}';
    }
}
