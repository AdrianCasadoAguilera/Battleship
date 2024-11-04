package com.client;

import java.io.IOException;

import org.json.JSONObject;

import com.client.clientUtils.CustomPopUp;
import com.client.clientUtils.UtilsViews;
import com.client.clientUtils.UtilsWS;
import com.client.ctrl.BattleController;
import com.client.ctrl.GameSetupController;
import com.client.ctrl.SearchingPlayersController;
import com.client.ctrl.StartController;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static UtilsWS wsClient;
    public static int id;
    public static int enemyId;
    public static String enemyName;

    public static int port = 3000;
    public static String protocol = "http";
    public static String host = "192.168.118.63";
    public static String protocolWS = "ws";

    @Override
    public void start(Stage primaryStage) {
        try {
            startServer();

            UtilsViews.addView(Main.class, "startView", "/assets/startView.fxml");
            UtilsViews.addView(Main.class, "searchPlayer", "/assets/searchingPlayers.fxml");
            UtilsViews.addView(Main.class, "putShipsView", "/assets/putShips.fxml");
            UtilsViews.addView(Main.class, "gameView", "/assets/gameView.fxml");

            Scene scene = new Scene(UtilsViews.parentContainer, 900, 500);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Battleship Game");

            primaryStage.setOnCloseRequest(event -> {
                if (wsClient != null) {
                    wsClient.forceExit(); 
                }
                Platform.exit();
                System.exit(0); 
            });

            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void startServer() throws IOException {
        wsClient = UtilsWS.getSharedInstance(protocolWS+"://"+host+":"+port);

        wsClient.onMessage((response) -> {
            Platform.runLater(() -> {
                JSONObject msgObj = new JSONObject(response);
                if(msgObj.getString("type").equals("accept name")){
                    Boolean showList = true;
                    if(msgObj.getString("action").equals("wait")){
                        showList = false;
                    }
                    id = msgObj.getInt("id");
                    StartController ctrl = (StartController) UtilsViews.getController("startView");
                    ctrl.acceptName(msgObj.getBoolean("success"),showList);
                }else if(msgObj.getString("type").equals("clientsList")){
                    System.out.println("Recibe lista");
                    SearchingPlayersController ctrl = (SearchingPlayersController) UtilsViews.getController("searchPlayer");
                    ctrl.receiveMessage(msgObj);
                }else if(msgObj.getString("type").equals("game request")){
                    CustomPopUp acceptPopUp = new CustomPopUp();
                    acceptPopUp.showPopup(id,msgObj.getInt("enemyId"),"The user "+msgObj.getString("name")+" challenged you to a game");
                }else if(msgObj.getString("type").equals("start game")){
                    enemyId = msgObj.getInt("enemyId");
                    UtilsViews.setView("putShipsView");
                }else if(msgObj.getString("type").equals("ships placed")){
                    GameSetupController ctrl = (GameSetupController) UtilsViews.getController("putShipsView");
                    ctrl.setEnemyShipsPlaced();
                }
            });
        });
    }
}
