package com.client.controllers;

import com.client.Main;
import com.client.UtilsViews;
import com.client.UtilsWS;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField nameInput;

    @FXML
    private TextField protocolInput;

    @FXML
    private TextField urlInput;

    @FXML
    private TextField portInput;

    @FXML
    private void connect() {
        String protocol = protocolInput.getText().trim();
        String url = urlInput.getText().trim();
        String port = portInput.getText().trim();
        String name = nameInput.getText().trim();

        if (protocol.isEmpty() || url.isEmpty() || port.isEmpty()) {
            System.out.println("Please fill in all fields.");
            return;
        }

        try {
            if(!portInput.getText().isEmpty()) {
                System.out.println("Connecting to " + protocol + "://" + url + ":" + port);
                UtilsWS.getSharedInstance(protocol + "://" + url + ":" + port, name);
            } else {
                System.out.println("Connecting to " + protocol + "://" + url);
                UtilsWS.getSharedInstance(protocol + "://" + url, name);
            }

            UtilsViews.setViewAnimating("lobby");
            LobbyController lobbyController = (LobbyController) UtilsViews.getController("lobby");
            lobbyController.setPlayerName("Welcome, " + name + "!");
            
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number.");
        }
    }
}
