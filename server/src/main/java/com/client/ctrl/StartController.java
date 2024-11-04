package com.client.ctrl;

import org.json.JSONObject;

import com.client.Main;
import com.client.clientUtils.UtilsViews;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

public class StartController {

    private String playerName;

    @FXML
    private TextField playerNameField;

    @FXML
    private Button searchPlayersButton;

    @FXML
    private Button waitPlayersButton;

    @FXML
    private Label errorPrompt;

    @FXML
    private void handleSearchPlayers() {
        playerName = playerNameField.getText();

        if (playerName.isEmpty()) {
            errorPrompt.setText("Please enter a name");
            System.out.println("Please enter a name.");
        } else {
            JSONObject obj = new JSONObject("{}");
            obj.put("type","name");
            obj.put("action","search");
            obj.put("name",playerName);
            Main.wsClient.safeSend(obj.toString());
        }
    }

    @FXML
    private void handleWaitPlayers(){
        playerName = playerNameField.getText();

        if (playerName.isEmpty()) {
            errorPrompt.setText("Please enter a name");
            System.out.println("Please enter a name.");
        } else {
            JSONObject obj = new JSONObject("{}");
            obj.put("type","name");
            obj.put("action","wait");
            obj.put("name",playerName);
            Main.wsClient.safeSend(obj.toString());
        }
    }

    public void acceptName(Boolean isAccepted, Boolean showList){
        if(isAccepted){
            System.out.println("Starting game for player: " + playerName);
            if(showList){
                SearchingPlayersController ctrl = (SearchingPlayersController) UtilsViews.getController("searchPlayer");
                ctrl.init();
                UtilsViews.setViewAnimating("searchPlayer");
            }else{
                searchPlayersButton.setDisable(true);
                waitPlayersButton.setDisable(true);
                playerNameField.setDisable(true);
                errorPrompt.setText("Waiting player...");
            }
            
        }else{
            errorPrompt.setText("The name \""+playerName+"\" is not available");
        }
    }
}
