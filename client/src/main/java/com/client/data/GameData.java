package com.client.data;

import java.util.ArrayList;
import org.json.JSONObject;

public class GameData {
    private static GameData instance = null;
    private ArrayList<ArrayList<String>> grid = new ArrayList<>();
    private ArrayList<JSONObject> myShips = new ArrayList<>();

    private GameData() {
        grid = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ArrayList<String> row = new ArrayList<>();
            for (int j = 0; j < 10; j++) {
                row.add("0");
            }
            grid.add(row);
        }
    }

    public static GameData getInstance() {
        if (instance == null) {
            instance = new GameData();
        }
        return instance;
    }

    public ArrayList<ArrayList<String>> getGrid() {
        return grid;
    }

    public void setMyShips(ArrayList<JSONObject> ships) {
        myShips = ships;
    }

    public ArrayList<JSONObject> getMyShips() {
        return myShips;
    }

    public boolean hasShipAt(int row, int col) {
        for (JSONObject ship : myShips) {
            int r = ship.getInt("row");
            int c = ship.getInt("col");
            int size = ship.getInt("size");
            boolean horizontal = ship.getString("orientation").equals("H");

            for (int i = 0; i < size; i++) {
                int sr = r + (horizontal ? 0 : i);
                int sc = c + (horizontal ? i : 0);
                if (sr == row && sc == col) {
                    return true;
                }
            }
        }
        return false;
    }
}
