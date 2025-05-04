package com.client.data;

import java.util.ArrayList;

public class GameData {
    private static GameData instance = null;
    private ArrayList<ArrayList<String>> grid = new ArrayList<>();

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

}
