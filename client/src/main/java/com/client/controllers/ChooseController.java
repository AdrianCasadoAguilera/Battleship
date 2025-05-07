package com.client.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.client.data.GameData;

import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ChooseController {

    @FXML
    private AnchorPane mainPane;

    @FXML
    private Pane dragLayer;

    @FXML
    private VBox shipsContainer;

    @FXML
    private Canvas canvas;

    private GraphicsContext gc;
    private ArrayList<ArrayList<String>> grid = new ArrayList<>();
    private Map<Rectangle, Integer> shipSizes = new HashMap<>();
    private Map<Rectangle, int[]> placedShips = new HashMap<>();
    private Map<Rectangle, Boolean> shipHorizontal = new HashMap<>();

    public void initialize() {
        gc = canvas.getGraphicsContext2D();
        GameData gameData = GameData.getInstance();
        grid = gameData.getGrid();
        drawGrid();
        generateShips();
    }

    public void drawGrid() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setStroke(Color.LIGHTGREY);

        double cellSize = canvas.getWidth() / grid.get(0).size();

        for (int row = 0; row < grid.size(); row++) {
            for (int col = 0; col < grid.get(row).size(); col++) {
                double x = col * cellSize;
                double y = row * cellSize;
                gc.setFill(grid.get(row).get(col).equals("S") ? Color.DARKBLUE : Color.GREY);
                gc.fillRect(x, y, cellSize, cellSize);
                gc.strokeRect(x, y, cellSize, cellSize);
            }
        }
    }

    public void generateShips() {
        int[] sizes = {2, 3, 4};
        double cellSize = canvas.getWidth() / grid.get(0).size();

        for (int size : sizes) {
            Rectangle ship = new Rectangle(cellSize * size, cellSize);
            ship.setFill(Color.DARKSLATEGRAY);
            ship.setArcWidth(10);
            ship.setArcHeight(10);

            shipSizes.put(ship, size);
            shipHorizontal.put(ship, true);

            final double[] offsetX = new double[1];
            final double[] offsetY = new double[1];
            final VBox[] originalParent = new VBox[1];
            final int[] originalIndex = new int[1];

            // ROTACIÓ amb clic dret si està al VBox
            ship.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY && ship.getParent() == shipsContainer) {
                    boolean isHorizontal = shipHorizontal.get(ship);
                    shipHorizontal.put(ship, !isHorizontal);

                    double w = ship.getWidth();
                    double h = ship.getHeight();
                    ship.setWidth(h);
                    ship.setHeight(w);
                }
            });

            ship.setOnMousePressed(event -> {
                // ❌ Bloquejar drag amb clic dret
                if (event.getButton() != MouseButton.PRIMARY) {
                    event.consume();
                    return;
                }

                originalParent[0] = shipsContainer;
                originalIndex[0] = shipsContainer.getChildren().indexOf(ship);

                offsetX[0] = event.getX();
                offsetY[0] = event.getY();

                if (placedShips.containsKey(ship)) {
                    int[] oldPos = placedShips.get(ship);
                    int shipSize = shipSizes.get(ship);
                    boolean isHorizontal = shipHorizontal.get(ship);

                    for (int i = 0; i < shipSize; i++) {
                        int r = oldPos[0] + (isHorizontal ? 0 : i);
                        int c = oldPos[1] + (isHorizontal ? i : 0);
                        grid.get(r).set(c, "0");
                    }

                    placedShips.remove(ship);
                }

                if (ship.getParent() != dragLayer) {
                    if (ship.getParent() != null)
                        ((Pane) ship.getParent()).getChildren().remove(ship);
                    dragLayer.getChildren().add(ship);
                }

                double localX = dragLayer.screenToLocal(event.getScreenX(), event.getScreenY()).getX() - offsetX[0];
                double localY = dragLayer.screenToLocal(event.getScreenX(), event.getScreenY()).getY() - offsetY[0];
                ship.setLayoutX(localX);
                ship.setLayoutY(localY);

                drawGrid();
            });

            ship.setOnMouseDragged(event -> {
                if (event.getButton() != MouseButton.PRIMARY) {
                    event.consume();
                    return;
                }

                double localX = dragLayer.screenToLocal(event.getScreenX(), event.getScreenY()).getX() - offsetX[0];
                double localY = dragLayer.screenToLocal(event.getScreenX(), event.getScreenY()).getY() - offsetY[0];
                ship.setLayoutX(localX);
                ship.setLayoutY(localY);
            });

            ship.setOnMouseReleased(event -> {
                if (event.getButton() != MouseButton.PRIMARY) {
                    event.consume();
                    return;
                }

                double sceneX = event.getScreenX();
                double sceneY = event.getScreenY();

                Point2D canvasScenePos = canvas.localToScene(0, 0);
                Point2D canvasInMain = mainPane.sceneToLocal(canvasScenePos);

                double cellSizeLocal = canvas.getWidth() / grid.get(0).size();

                double localX = event.getSceneX() - canvasScenePos.getX();
                double localY = event.getSceneY() - canvasScenePos.getY();

                int col = (int) (localX / cellSizeLocal);
                int row = (int) (localY / cellSizeLocal);
                int shipSize = shipSizes.get(ship);
                boolean isHorizontal = shipHorizontal.get(ship);

                boolean fits = row >= 0 && col >= 0 &&
                        (isHorizontal
                                ? (col + shipSize <= grid.get(0).size() && row < grid.size())
                                : (row + shipSize <= grid.size() && col < grid.get(0).size()));

                if (fits && !intersectsAnotherShip(row, col, shipSize, isHorizontal)) {
                    for (int i = 0; i < shipSize; i++) {
                        int r = row + (isHorizontal ? 0 : i);
                        int c = col + (isHorizontal ? i : 0);
                        grid.get(r).set(c, "S");
                    }

                    placedShips.put(ship, new int[]{row, col});
                    dragLayer.getChildren().remove(ship);
                    mainPane.getChildren().add(ship);

                    double x = canvasInMain.getX() + col * cellSizeLocal;
                    double y = canvasInMain.getY() + row * cellSizeLocal;
                    ship.setLayoutX(x);
                    ship.setLayoutY(y);
                } else {
                    // Tornar al VBox
                    dragLayer.getChildren().remove(ship);
                    shipsContainer.getChildren().add(ship);

                    shipHorizontal.put(ship, true);
                    ship.setWidth(cellSizeLocal * shipSize);
                    ship.setHeight(cellSizeLocal);

                    ship.setLayoutX(0);
                    ship.setLayoutY(0);
                }

                drawGrid();
            });

            shipsContainer.getChildren().add(ship);
        }
    }

    private boolean intersectsAnotherShip(int row, int col, int length, boolean horizontal) {
        for (int i = 0; i < length; i++) {
            int r = row + (horizontal ? 0 : i);
            int c = col + (horizontal ? i : 0);

            if (r < 0 || r >= grid.size() || c < 0 || c >= grid.get(0).size()) return true;
            if (!grid.get(r).get(c).equals("0")) return true;
        }
        return false;
    }
}
