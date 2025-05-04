package com.server;

import org.json.JSONObject;

public class GameData {

    private JSONObject gameData = new JSONObject();

    private GameData() {
        // Private constructor to prevent instantiation
    }
    private static GameData instance = null;

    public static GameData getInstance() {
        if (instance == null) {
            instance = new GameData();
        }
        return instance;
    }

    public void addPlayer(String playerName) {
        gameData.put(playerName, new JSONObject());
    }
}
