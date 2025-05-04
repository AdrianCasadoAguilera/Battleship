package com.client.controllers;

import java.util.ArrayList;

import com.client.data.GameData;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class ChooseController {
    
    @FXML
    private Canvas canvas;

    private GraphicsContext gc = null;

    private ArrayList<ArrayList<String>> grid = new ArrayList<>();

    public void initialize() {
        gc = canvas.getGraphicsContext2D();
        GameData gameData = GameData.getInstance();
        grid = gameData.getGrid();
        drawGrid();
    }


    public void drawGrid() {
        gc.setStroke(Color.LIGHTGREY);

        for (int row = 0; row < grid.size(); row++) {
            for (int col = 0; col < grid.get(row).size(); col++) {
                double cellSize = canvas.getWidth() / grid.get(row).size();
                double x = col * cellSize;
                double y = row * cellSize;
                gc.setFill(Color.GREY);
                gc.fillRect(x, y, cellSize, cellSize);
                gc.strokeRect(x, y, cellSize, cellSize);
            }
        }
    }
}
