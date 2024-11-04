package com.client.ctrl;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.client.Main;
import com.client.clientUtils.Position;
import com.client.clientUtils.ShipPosition;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class BattleController {

    @FXML
    private GridPane enemyBoard;

    @FXML
    private GridPane playerBoard;

    // Colores para cada barco
    private final String[] shipColors = {"#FFA07A", "#20B2AA", "#778899", "#FFD700"};

    @FXML
    public void initialize() {
        // Inicializar el tablero enemigo para los ataques
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                Label cell = new Label();
                cell.setPrefSize(40, 40);
                cell.setStyle("-fx-border-color: black; -fx-background-color: white;");
                
                // Crear variables finales para usarlas en la lambda
                final int attackRow = row;
                final int attackCol = col;
                
                cell.setOnMouseClicked(event -> handleAttack(attackRow, attackCol));
                enemyBoard.add(cell, col, row);
            }
        }
    
        // Inicializar el tablero propio (playerBoard) para mostrar los barcos
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                Label cell = new Label();
                cell.setPrefSize(40, 40);
                cell.setStyle("-fx-border-color: black;");
                playerBoard.add(cell, col, row);
            }
        }
    }

    // Método para inicializar el tablero con la disposición de los barcos
    public void inicializarTablero(List<ShipPosition> placedShips) {
        int colorIndex = 0;

        for(ShipPosition ship : placedShips){
            List<Position> positions = ship.getPositions();
            
            for (Position position : positions) {
                int row = position.getRow();
                int col = position.getCol();

                System.out.println("row: "+row+", col: "+col);
                
                Label celda = (Label) playerBoard.getChildren().get(row * 10 + col);
                celda.setStyle("-fx-background-color: " + shipColors[colorIndex] + "; -fx-border-color: black;");
            }

            colorIndex++;
        }
    
    }

    public void inicializarTableroEnemigo(String placedShipsString) {
        int colorIndex = 0;

        List<ShipPosition> placedShips = parseShipPositions(placedShipsString);

        for(ShipPosition ship : placedShips){
            List<Position> positions = ship.getPositions();
            
            for (Position position : positions) {
                int row = position.getRow();
                int col = position.getCol();

                System.out.println("row: "+row+", col: "+col);
                
                Label celda = (Label) enemyBoard.getChildren().get(row * 10 + col);
                celda.setStyle("-fx-background-color: " + shipColors[colorIndex] + "; -fx-border-color: black;");
            }

            colorIndex++;
        }
    
    }

    private void handleAttack(int row, int col) {
        System.out.println("Attacking position: (" + row + ", " + col + ")");

        JSONObject obj = new JSONObject("{}");



        Main.wsClient.safeSend(obj.toString());
    }

    public void receiveMessage(JSONObject obj) {
        String type = obj.getString("type");
        
        if(type.equals("enemy_positions")){
            String msg = obj.getString("message");

            inicializarTablero(parseShipPositions(msg));
        }
    }

    public static ArrayList<ShipPosition> parseShipPositions(String jsonString) {
        ArrayList<ShipPosition> placedShips = new ArrayList<>();

        JSONObject obj = new JSONObject(jsonString);

        int shipIndex = 1;
        while (obj.has("ship" + shipIndex)) {
            JSONObject shipObj = obj.getJSONObject("ship" + shipIndex);
            List<Position> positions = new ArrayList<>();

            int posIndex = 1;
            while (shipObj.has("pos" + posIndex)) {
                JSONObject posObj = shipObj.getJSONObject("pos" + posIndex);
                int col = posObj.getInt("col");
                int row = posObj.getInt("row");

                Position position = new Position(row, col);
                positions.add(position);
                posIndex++;
            }

            ShipPosition ship = new ShipPosition(positions.size(), positions);
            placedShips.add(ship);
            shipIndex++;
        }

        return placedShips;
    }
}
