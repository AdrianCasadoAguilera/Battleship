package com.client.controllers;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

import com.client.UtilsWS;
import com.client.data.GameData;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;

public class GameController {

    @FXML
    private AnchorPane mainPane;

    @FXML
    private Canvas myGrid;

    @FXML
    private Canvas enemyGrid;

    private final int gridSize = 10;
    private double cellSize;

    private Set<String> clickedCells = new HashSet<>();
    private String hoverCell = null;

    private Set<String> enemyHits = new HashSet<>();
    private Set<String> enemyMisses = new HashSet<>();

    private Set<String> myHits = new HashSet<>();
    private Set<String> myMisses = new HashSet<>();


    public void initialize() {
        drawMyGrid();
        drawEnemyGrid();

        // Event listeners per a l'enemic
        enemyGrid.setOnMouseMoved(this::handleMouseMoved);
        enemyGrid.setOnMouseExited(e -> {
            hoverCell = null;
            drawEnemyGrid();
        });
        enemyGrid.setOnMouseClicked(this::handleMouseClicked);
    }

    private void drawMyGrid() {
        GraphicsContext gc = myGrid.getGraphicsContext2D();
        gc.clearRect(0, 0, myGrid.getWidth(), myGrid.getHeight());
    
        cellSize = myGrid.getWidth() / 10;
        gc.setStroke(Color.LIGHTGRAY);
    
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                double x = col * cellSize;
                double y = row * cellSize;
                String cellKey = row + "," + col;
    
                if (myHits.contains(cellKey)) {
                    gc.setFill(Color.RED);
                } else if (myMisses.contains(cellKey)) {
                    gc.setFill(Color.DARKSLATEGRAY);
                } else if (GameData.getInstance().hasShipAt(row, col)) {
                    gc.setFill(Color.DARKGRAY);
                } else {
                    gc.setFill(Color.GREY);
                }
    
                gc.fillRect(x, y, cellSize, cellSize);
                gc.strokeRect(x, y, cellSize, cellSize);
            }
        }
    }
    
    

    private void drawEnemyGrid() {
        GraphicsContext gc = enemyGrid.getGraphicsContext2D();
        gc.clearRect(0, 0, enemyGrid.getWidth(), enemyGrid.getHeight());
    
        cellSize = enemyGrid.getWidth() / gridSize;
        gc.setStroke(Color.LIGHTGRAY);
    
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                double x = col * cellSize;
                double y = row * cellSize;
                String cellKey = row + "," + col;
    
                if (enemyHits.contains(cellKey)) {
                    gc.setFill(Color.RED); // toc
                } else if (enemyMisses.contains(cellKey)) {
                    gc.setFill(Color.DARKSLATEGRAY); // aigua
                } else if (hoverCell != null && hoverCell.equals(cellKey)) {
                    gc.setFill(Color.LIGHTGRAY); // hover
                } else {
                    gc.setFill(Color.GREY); // neutral
                }
    
                gc.fillRect(x, y, cellSize, cellSize);
                gc.strokeRect(x, y, cellSize, cellSize);
            }
        }
    }
    

    private void handleMouseMoved(MouseEvent event) {
        int col = (int)(event.getX() / cellSize);
        int row = (int)(event.getY() / cellSize);
        hoverCell = row + "," + col;
        drawEnemyGrid();
    }

    private void handleMouseClicked(MouseEvent event) {
    int col = (int)(event.getX() / cellSize);
    int row = (int)(event.getY() / cellSize);
    String cellKey = row + "," + col;

    if (!clickedCells.contains(cellKey)) {
        clickedCells.add(cellKey);
        drawEnemyGrid();

        // Envia l'atac pel WebSocket
        JSONObject attackMsg = new JSONObject();
        attackMsg.put("type", "attack");

        JSONObject data = new JSONObject();
        data.put("row", row);
        data.put("col", col);
        attackMsg.put("data", data);

        UtilsWS.getSharedInstance("", "").safeSend(attackMsg.toString());
        }
    }

    public void handleAttackResult(int row, int col, boolean hit) {
        String key = row + "," + col;
        if (hit) {
            enemyHits.add(key);
        } else {
            enemyMisses.add(key);
        }
        drawEnemyGrid();
    }
    
    public void handleGotAttacked(int row, int col, boolean hit) {
        String key = row + "," + col;
        if (hit) {
            myHits.add(key);
        } else {
            myMisses.add(key);
        }
        drawMyGrid();
    }
    
}
