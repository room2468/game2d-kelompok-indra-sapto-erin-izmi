package com.garticphone.shared;

import java.util.List;

public class ReplayPayload {
    public List<List<String>> chains;
    public List<PlayerData> players;

    public ReplayPayload(List<List<String>> chains, List<PlayerData> players) {
        this.chains = chains;
        this.players = players;
    }
}
