package com.client.clientUtils;

import org.json.JSONObject;

import com.client.Main;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CustomPopUp {

    public void showPopup(int hostId,int enemyId,String message) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Game request");

        Label messageLabel = new Label(message);
        Button acceptButton = new Button("Accept");
        Button denyButton = new Button("Deny");

        acceptButton.setOnAction(e -> {
            JSONObject obj = new JSONObject("{}");

            obj.put("type","game request");
            obj.put("status","accepted");
            obj.put("hostId",hostId);
            obj.put("enemyId",enemyId);

            Main.wsClient.safeSend(obj.toString());
            popupStage.close();
        });

        denyButton.setOnAction(e -> {
            System.out.println("Denegar presionado");
            popupStage.close();
        });

        VBox layout = new VBox(10);
        layout.getChildren().addAll(messageLabel, acceptButton, denyButton);
        layout.setStyle("-fx-padding: 10; -fx-alignment: center;");

        Scene scene = new Scene(layout, 250, 150);
        popupStage.setScene(scene);
        popupStage.showAndWait();
    }
}
