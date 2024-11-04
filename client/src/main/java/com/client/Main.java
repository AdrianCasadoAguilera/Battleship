package com.client;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    public static UtilsWS wsClient;

    public static int port = 3000;
    public static String protocol = "http";
    public static String host = "localhost";
    public static String protocolWS = "ws";

    public static void main(String[] args) {
        wsClient = UtilsWS.getSharedInstance(protocolWS + "://" + host + ":" + port);
        wsClient.onMessage((response)->{

        });
    }

    @Override
    public void start(Stage arg0) throws Exception {
        System.out.println("started");
    }
    
}