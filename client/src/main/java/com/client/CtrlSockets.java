package com.client;

import java.net.URL;
import java.util.ResourceBundle;

import org.json.JSONObject;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;

public class CtrlSockets implements Initializable{



    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        
    }

    @FXML
    public void sendAttack(int r, int c){
        JSONObject msg = new JSONObject("{}");

        msg.put("type", "attack");
        msg.put("position",new int[]{r,c});

        Main.wsClient.safeSend(msg.toString());
        System.out.println("Attack sent: x="+r+",y="+c);
    }
    
}
