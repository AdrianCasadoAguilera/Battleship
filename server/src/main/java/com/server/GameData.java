package com.server;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameData {

    private Map<String, JSONArray> playerShips = new ConcurrentHashMap<>();

    private GameData() {}

    private static GameData instance = null;

    public static GameData getInstance() {
        if (instance == null) {
            instance = new GameData();
        }
        return instance;
    }

    public void addPlayer(String playerName) {
        playerShips.put(playerName, new JSONArray());
    }

    public void setShips(String playerName, JSONArray ships) {
        playerShips.put(playerName, ships);
    }

    public boolean allPlayersReady(int expectedPlayers) {
        return playerShips.size() == expectedPlayers &&
               playerShips.values().stream().allMatch(array -> array.length() > 0);
    }

    public Map<String, JSONArray> getAllShips() {
        return playerShips;
    }

    public boolean isHit(String playerName, int row, int col) {
        JSONArray ships = playerShips.get(playerName);
        for (int i = 0; i < ships.length(); i++) {
            JSONObject ship = ships.getJSONObject(i);
            int r = ship.getInt("row");
            int c = ship.getInt("col");
            int size = ship.getInt("size");
            boolean horizontal = ship.getString("orientation").equals("H");
    
            for (int j = 0; j < size; j++) {
                int sr = r + (horizontal ? 0 : j);
                int sc = c + (horizontal ? j : 0);
                if (sr == row && sc == col) return true;
            }
        }
        return false;
    }
    
}
