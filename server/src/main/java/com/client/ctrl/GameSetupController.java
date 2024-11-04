package com.client.ctrl;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import com.client.Main;
import com.client.clientUtils.Position;
import com.client.clientUtils.ShipPosition;
import com.client.clientUtils.UtilsViews;

public class GameSetupController {

    @FXML
    private GridPane battleGrid;

    @FXML
    private HBox ship2, ship3, ship4, ship5;

    private int selectedShipSize = 0;
    private boolean isHorizontal = true;  // Dirección del barco (horizontal o vertical)
    private final List<ShipPosition> placedShips = new ArrayList<>();  // Lista para guardar posiciones de barcos
    private final Set<Position> occupiedCells = new HashSet<>(); // Celdas ocupadas por los barcos colocados

    @FXML
    public void initialize() {
        // Configurar eventos para seleccionar barcos
        ship2.setOnMouseClicked(event -> selectShip(ship2, 2));
        ship3.setOnMouseClicked(event -> selectShip(ship3, 3));
        ship4.setOnMouseClicked(event -> selectShip(ship4, 4));
        ship5.setOnMouseClicked(event -> selectShip(ship5, 5));
        
        // Agregar eventos de mouse sobre la cuadrícula para mostrar el barco
        battleGrid.setOnMouseMoved(this::handleMouseHover);
        battleGrid.setOnMouseClicked(this::handleGridClick);
    }

    private void selectShip(HBox ship, int size) {
        // Si el barco ya está colocado, no permitir seleccionar de nuevo
        if (ship.isDisabled()) {
            System.out.println("This ship has already been placed.");
            return;
        }
        
        selectedShipSize = size;
        System.out.println("Selected ship size: " + size);
    }

    private void handleMouseHover(MouseEvent event) {
        if (selectedShipSize == 0) {
            return;  // No hay ningún barco seleccionado
        }
        
        clearGridHighlights();  // Limpiar cualquier resaltado previo

        int row = (int) (event.getY() / 40);  // Convertir la posición Y en índice de fila
        int col = (int) (event.getX() / 40);  // Convertir la posición X en índice de columna

        // Calcular la extensión del barco en la cuadrícula y resaltar las celdas
        for (int i = 0; i < selectedShipSize; i++) {
            int r = row + (isHorizontal ? 0 : i);
            int c = col + (isHorizontal ? i : 0);
            if (r < 10 && c < 10) {
                Position position = new Position(r, c);
                if (!occupiedCells.contains(position)) { // Solo resaltar si no está ocupado
                    Label cell = (Label) getNodeFromGridPane(battleGrid, c, r);
                    if (cell != null) {
                        cell.setStyle("-fx-background-color: lightgreen;");
                    }
                }
            }
        }
    }

    private void handleGridClick(MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY) {  // Clic derecho para rotar
            toggleOrientation();
            handleMouseHover(event);  // Actualizar la visualización después de rotar
            return;
        }

        if (selectedShipSize == 0) {
            return;  // No hay ningún barco seleccionado
        }
        
        int row = (int) (event.getY() / 40);  // Convertir la posición Y en índice de fila
        int col = (int) (event.getX() / 40);  // Convertir la posición X en índice de columna

        // Verificar que el barco cabe dentro de la cuadrícula y no se superpone
        List<Position> currentShipPositions = new ArrayList<>();
        for (int i = 0; i < selectedShipSize; i++) {
            int r = row + (isHorizontal ? 0 : i);
            int c = col + (isHorizontal ? i : 0);
            
            // Verificar si está fuera de los límites de la cuadrícula
            if (r >= 10 || c >= 10) {
                System.out.println("Cannot place ship: Out of grid bounds.");
                return;  // Salir si alguna parte del barco está fuera de los límites
            }

            Position position = new Position(r, c);
            
            // Verificar si la posición ya está ocupada
            if (occupiedCells.contains(position)) {
                System.out.println("Cannot place ship: Position already occupied.");
                return;  // Salir si alguna parte del barco se superpone
            }

            currentShipPositions.add(position);
        }

        // Colocar el barco en la cuadrícula y marcar sus posiciones
        for (Position pos : currentShipPositions) {
            Label cell = (Label) getNodeFromGridPane(battleGrid, pos.getCol(), pos.getRow());
            if (cell != null) {
                cell.setStyle("-fx-background-color: darkgrey;");  // Color final del barco en la cuadrícula
            }
            occupiedCells.add(pos);  // Añadir la posición a las celdas ocupadas
        }

        // Guardar la posición del barco y deshabilitar el barco del menú lateral
        placedShips.add(new ShipPosition(selectedShipSize, currentShipPositions));
        disableShipInMenu(selectedShipSize);

        selectedShipSize = 0;  // Desseleccionar el barco después de colocarlo
        
        // Verificar si todos los barcos han sido colocados
        if (allShipsPlaced()) {
            BattleController battleController = (BattleController) UtilsViews.getController("gameView");

            // Pasa la disposición de los barcos a la vista de ataque
            battleController.inicializarTablero(placedShips);

            JSONObject obj = new JSONObject("{}");

            obj.put("destination",Main.enemyId);
            obj.put("type", "positions");

            JSONObject msgObj = new JSONObject("{}");

            for(int i = 0; i < placedShips.size(); i++){
                ShipPosition ship = placedShips.get(i);
                JSONObject shipObj = new JSONObject("{}");
                for(int j = 0; j < ship.getPositions().size(); j++){
                    Position pos = ship.getPositions().get(j);
                    JSONObject shipPosObj = new JSONObject("{}");
                    shipPosObj.put("col", pos.getCol());
                    shipPosObj.put("row",pos.getRow());

                    shipObj.put("pos"+String.valueOf(j+1),shipPosObj);
                }
                msgObj.put("ship"+String.valueOf(i+1), shipObj);
            }

            obj.put("message",msgObj.toString());
            Main.wsClient.safeSend(obj.toString());          

            UtilsViews.setView("gameView");
        }
    }

    private boolean allShipsPlaced() {
        // Verifica si todos los barcos (2, 3, 4 y 5) han sido colocados
        return placedShips.size() == 4;
    }

    private void toggleOrientation() {
        isHorizontal = !isHorizontal;
        System.out.println("Ship orientation: " + (isHorizontal ? "Horizontal" : "Vertical"));
    }

    private void clearGridHighlights() {
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                Position position = new Position(row, col);
                if (!occupiedCells.contains(position)) { // Limpiar solo celdas no ocupadas
                    Label cell = (Label) getNodeFromGridPane(battleGrid, col, row);
                    if (cell != null) {
                        cell.setStyle("-fx-background-color: white; -fx-border-color: black;");
                    }
                }
            }
        }
    }

    private Label getNodeFromGridPane(GridPane gridPane, int col, int row) {
        for (javafx.scene.Node node : gridPane.getChildren()) {
            if (GridPane.getColumnIndex(node) == col && GridPane.getRowIndex(node) == row) {
                return (Label) node;
            }
        }
        return null;
    }

    private void disableShipInMenu(int shipSize) {
        // Deshabilitar el barco en el menú según su tamaño
        switch (shipSize) {
            case 2 -> ship2.setDisable(true);
            case 3 -> ship3.setDisable(true);
            case 4 -> ship4.setDisable(true);
            case 5 -> ship5.setDisable(true);
        }
    }

}
