package com.server;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.json.JSONObject;

public class Main extends WebSocketServer {

    private static String host = "192.168.118.63";
    private static int port = 3000;

    private Map<WebSocket, Integer> clients;
    private Map<Integer, String> clientNames;
    private Map<Integer, String> waitingClients;
    private int lastId = 0;

    public Main(InetSocketAddress address){
        super(address);
        clientNames = new HashMap<>();
        clients = new HashMap<>();
        waitingClients = new HashMap<>();
    }

    public static void main(String[] args) {
        Main server = new Main(new InetSocketAddress(host,port));

        server.start();

        LineReader reader = LineReaderBuilder.builder().build();

        System.out.println("Server running. Type 'exit' to gracefully stop it.");

        try {
            while (true) {
                String line = null;
                try {
                    line = reader.readLine("> ");
                } catch (UserInterruptException e) {
                    continue;
                } catch (EndOfFileException e) {
                    break;
                }

                line = line.trim();

                if (line.equalsIgnoreCase("exit")) {
                    System.out.println("Stopping server...");
                    try {
                        server.stop(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                } else {
                    System.out.println("Unknown command. Type 'exit' to stop server gracefully.");
                }
            }
        } finally {
            System.out.println("Server stopped.");
        }
    }

    @Override
    public void onClose(WebSocket client, int arg1, String arg2, boolean arg3) {
        System.out.println("Client exited: "+client);
        clientNames.remove(clients.get(client));
        waitingClients.remove(clients.get(client));
        clients.remove(client);

        JSONObject response = new JSONObject("{}");
        response.put("type","clientsList");
        JSONObject msgResponse = new JSONObject("{}");
        for(int key : waitingClients.keySet()){
            msgResponse.put(String.valueOf(key),waitingClients.get(key));
        }
        response.put("list",msgResponse);
        broadcastMessage(response.toString(),client);
    }

    @Override
    public void onError(WebSocket arg0, Exception arg1) {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'onError'");
    }

    @Override
    public void onMessage(WebSocket conn, String msg) {
        JSONObject obj = new JSONObject(msg);
        String type = obj.getString("type");

        if (type.equals("attack")) {
            // TODO Gestionar ataque
        } else if (type.equals("name")) {
            String newName = obj.getString("name");
            JSONObject duplicatedNameObj = new JSONObject("{}");
            duplicatedNameObj.put("type", "accept name");

            if (!clientNames.values().contains(newName)) {
                clientNames.put(clients.get(conn), newName);
                duplicatedNameObj.put("id",clients.get(conn));
                duplicatedNameObj.put("action", obj.getString("action"));
                duplicatedNameObj.put("success", true);

                JSONObject response = new JSONObject("{}");
                response.put("type", "clientsList");
                JSONObject msgResponse = new JSONObject("{}");
                
                for (int key : waitingClients.keySet()) {
                    msgResponse.put(String.valueOf(key), waitingClients.get(key));
                }
                
                response.put("list", msgResponse);
                broadcastMessage(response.toString(), conn);
            } else {
                duplicatedNameObj.put("success", false);
            }

            if(obj.getString("action").equals("wait")){
                waitingClients.put(clients.get(conn), newName);
                System.out.println(waitingClients);

                JSONObject response = new JSONObject("{}");
                response.put("type", "clientsList");
                JSONObject msgResponse = new JSONObject("{}");
                
                for (int key : waitingClients.keySet()) {
                    msgResponse.put(String.valueOf(key), waitingClients.get(key));
                }
                
                response.put("list", msgResponse);
                conn.send(response.toString());
            }
            duplicatedNameObj.put("action",obj.getString("action"));
            
            conn.send(duplicatedNameObj.toString());
        } else if (type.equals("getClients")) {
            JSONObject response = new JSONObject("{}");
            response.put("type", "clientsList");
            JSONObject msgResponse = new JSONObject("{}");
            
            for (int key : waitingClients.keySet()) {
                msgResponse.put(String.valueOf(key), waitingClients.get(key));
            }
            
            response.put("list", msgResponse);
            conn.send(response.toString());
        }else if(type.equals("join game")){
            int gameId = obj.getInt("id");
            JSONObject objToSend = new JSONObject("{}");
            objToSend.put("type","game request");
            objToSend.put("status","pending");
            objToSend.put("enemyId",clients.get(conn));
            objToSend.put("name",clientNames.get(clients.get(conn)));

            sendPrivateMessage(gameId, objToSend.toString(), conn);
        }else if(type.equals("game request")){
            if(obj.getString("status").equals("accepted")){
                int hostId = obj.getInt("hostId");
                int enemyId = obj.getInt("enemyId");

                JSONObject objToSend = new JSONObject("{}");
                objToSend.put("type","start game");
                objToSend.put("enemyId",hostId);

                sendPrivateMessage(enemyId, objToSend.toString(), conn);

                objToSend.put("enemyId",enemyId);
                sendPrivateMessage(hostId, objToSend.toString(), conn);
            }
        }else if(type.equals("ships placed")){
            sendPrivateMessage(obj.getInt("enemyId"), obj.toString(), conn);
        }
    }

    @Override
    public void onOpen(WebSocket client, ClientHandshake arg1) {
        System.out.println("New client connected: "+client);
        lastId++;
        clients.put(client, lastId);
    }

    @Override
    public void onStart() {
        System.out.println("Server started at port "+port);
        setConnectionLostTimeout(1000);
    }

    private void sendPrivateMessage(int destination, String message, WebSocket senderConn) {
        boolean found = false;

        for (Map.Entry<WebSocket, Integer> entry : clients.entrySet()) {
            if (entry.getValue() == destination) {
                found = true;
                try {
                    entry.getKey().send(message);
                    JSONObject confirmation = new JSONObject("{}");
                    confirmation.put("type", "enemy_positions");
                    confirmation.put("message", message);
                    senderConn.send(confirmation.toString());
                } catch (WebsocketNotConnectedException e) {
                    System.out.println("Client " + destination + " not connected.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }

        if (!found) {
            System.out.println("Client " + destination + " not found.");
            // notifySenderClientUnavailable(senderConn, destination);
        }
    }

    private void broadcastMessage(String message, WebSocket sender) {
        for (Map.Entry<WebSocket, Integer> entry : clients.entrySet()) {
            WebSocket conn = entry.getKey();
            if (conn != sender) {
                try {
                    conn.send(message);
                } catch (WebsocketNotConnectedException e) {
                    System.out.println("Client " + entry.getValue() + " not connected.");
                    clients.remove(conn);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}