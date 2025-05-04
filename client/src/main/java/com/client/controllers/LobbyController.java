package com.client.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class LobbyController {
    @FXML
    private Text playerName;

    @FXML
    private Text waitingText;

    private int seconds = 5;

    public void setPlayerName(String name) {
        playerName.setText(name);
    }

    public void startWaiting() {
        seconds = 5; 
        waitingText.setText("Starting in "+seconds+"s...");
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            seconds--;
            if(seconds < 0) {
                waitingText.setText("Starting...");
            }
            waitingText.setText("Starting in "+seconds+"s...");
        }));
        timeline.setCycleCount(5); 
        timeline.play();
    }
}
