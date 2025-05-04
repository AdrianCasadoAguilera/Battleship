package com.client;

import com.client.controllers.LobbyController;
import com.client.controllers.LoginController;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static UtilsWS wsClient;

    public static int port = 3000;
    public static String protocol = "http";
    public static String host = "localhost";
    public static String protocolWS = "ws";

    public static LoginController loginController;
    public static LobbyController lobbyController;

    public static void main(String[] args) throws Exception {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        final int windowWidth = 400;
        final int windowHeight = 300;
        UtilsViews.addView(getClass(), "login", "/assets/layouts/login.fxml");
        UtilsViews.addView(getClass(), "lobby", "/assets/layouts/lobby.fxml");
        UtilsViews.addView(getClass(), "choose", "/assets/layouts/choose.fxml");

        loginController = (LoginController) UtilsViews.getController("Login");
        lobbyController = (LobbyController) UtilsViews.getController("Lobby");

        Scene scene = new Scene(UtilsViews.parentContainer);
        stage.setScene(scene);
        stage.onCloseRequestProperty(); 
        stage.setTitle("BattleShip");
        stage.setMinWidth(windowWidth);
        stage.setMinHeight(windowHeight);
        stage.show();
    }

    @Override
    public void stop() {
        if (wsClient != null) {
            wsClient.forceExit();
        }
        System.exit(1); 
    }
    
}