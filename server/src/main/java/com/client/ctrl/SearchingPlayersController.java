package com.client.ctrl;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONObject;

import com.client.Main;
import com.client.clientUtils.UtilsViews;

public class SearchingPlayersController {

    @FXML
    private ListView<HBox> playerListView;

    private Map<Integer, String> clientNames = new HashMap<>();

    public void init() {
        JSONObject petition = new JSONObject("{}");
        petition.put("type", "getClients");
        Main.wsClient.safeSend(petition.toString());
    }

    private void loadPlayers() {
        playerListView.getItems().clear();

        clientNames.forEach((id, name) -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/playerListItem.fxml"));
                HBox playerItem = loader.load();

                Label playerNameLabel = (Label) playerItem.lookup("#playerNameLabel");
                Button actionButton = (Button) playerItem.lookup("#actionButton");

                playerNameLabel.setText(name);
                actionButton.setOnAction(event -> handlePlayerAction(id, name));

                playerListView.getItems().add(playerItem);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void handlePlayerAction(Integer id, String name) {

        Main.enemyId = id;
        Main.enemyName = name;

        System.out.println("Action clicked for player: " + name + " with ID: " + id);
        JSONObject obj = new JSONObject("{}");
        obj.put("type","join game");
        obj.put("id",id);

        Main.wsClient.safeSend(obj.toString());
    }

    public void receiveMessage(JSONObject jsonObject){

        if(jsonObject.getString("type").equals("clientsList")){
            JSONObject listObject = jsonObject.getJSONObject("list");

            Iterator<String> keys = listObject.keys();

            clientNames.clear();
            while (keys.hasNext()) {
                String key = keys.next();
                int clientId = Integer.parseInt(key);
                String clientName = listObject.getString(key);

                clientNames.put(clientId, clientName);
            }

            System.out.println(clientNames);

            loadPlayers();
        }else if(jsonObject.getString("type").equals("enemy accepted")){
            if(jsonObject.getBoolean("value")){
                UtilsViews.setView("putShipsView");
            }
        }
        
    }
}
