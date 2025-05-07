package com.server;

import org.java_websocket.server.WebSocketServer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Iterator;
import java.util.Map;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import org.json.JSONArray;
import org.json.JSONObject;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

public class Main extends WebSocketServer {

    private Map<WebSocket, String> clients;
    private Map<String, WebSocket> nameToSocket = new ConcurrentHashMap<>();
    private String currentPlayer = null;

    public Main(InetSocketAddress address) {
        super(address);
        clients = new ConcurrentHashMap<>();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("WebSocket client connected");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String clientName = clients.get(conn);
        clients.remove(conn);
        nameToSocket.remove(clientName);
        System.out.println("WebSocket client disconnected: " + clientName);
        sendClientsList();
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        String clientName = clients.get(conn);
        JSONObject obj = new JSONObject(message);

        if (obj.has("type")) {
            String type = obj.getString("type");
            switch (type) {
                case "register":
                    String name = obj.getString("name");
                    clients.put(conn, name);
                    nameToSocket.put(name, conn);
                    GameData.getInstance().addPlayer(name);
                    System.out.println("Client registered: " + name);
                    sendClientsList();

                    if (clients.size() == 2) {
                        JSONObject rst = new JSONObject();
                        rst.put("type", "starting");
                        rst.put("message", "5s to start");

                        for (Map.Entry<WebSocket, String> entry : clients.entrySet()) {
                            sendSafely(entry.getKey(), rst);
                        }

                        ScheduledExecutorService timeout = Executors.newSingleThreadScheduledExecutor();
                        timeout.schedule(() -> {
                            JSONObject start = new JSONObject();
                            start.put("type", "start");
                            start.put("message", "Game started");
                            for (Map.Entry<WebSocket, String> entry : clients.entrySet()) {
                                sendSafely(entry.getKey(), start);
                            }
                        }, 5, TimeUnit.SECONDS);
                    }
                    break;

                case "ready":
                    if (clientName == null) return;

                    if (obj.has("ships")) {
                        JSONArray ships = obj.getJSONArray("ships");
                        GameData.getInstance().setShips(clientName, ships);
                        System.out.println("Player " + clientName + " is ready with ships: " + ships);

                        if (GameData.getInstance().allPlayersReady(2)) {
                            System.out.println("All players ready. Starting game.");

                            JSONObject startGameMsg = new JSONObject();
                            startGameMsg.put("type", "startGame");

                            for (Map.Entry<WebSocket, String> entry : clients.entrySet()) {
                                sendSafely(entry.getKey(), startGameMsg);
                            }

                            // Seleccionar jugador inicial aleatoriament
                            String[] names = clients.values().toArray(new String[0]);
                            currentPlayer = names[(int) (Math.random() * 2)];

                            sendTurnInfo();
                        }
                    }
                    break;

                case "attack":
                    if (!clientName.equals(currentPlayer)) {
                        JSONObject err = new JSONObject();
                        err.put("type", "error");
                        err.put("message", "Not your turn!");
                        sendSafely(conn, err);
                        return;
                    }

                    String opponent = clients.values().stream()
                        .filter(n -> !n.equals(clientName))
                        .findFirst().orElse(null);


                    if (opponent != null && nameToSocket.containsKey(opponent)) {
                        JSONObject data = obj.getJSONObject("data");
                        int row = data.getInt("row");
                        int col = data.getInt("col");

                        boolean hit = GameData.getInstance().isHit(opponent, row, col);

                        // Envia resultat a ambdós jugadors
                        JSONObject resultToAttacker = new JSONObject();
                        resultToAttacker.put("type", "attackResult");
                        resultToAttacker.put("row", row);
                        resultToAttacker.put("col", col);
                        resultToAttacker.put("hit", hit);

                        JSONObject resultToDefender = new JSONObject(resultToAttacker.toString());
                        resultToDefender.put("type", "gotAttacked");

                        sendSafely(nameToSocket.get(clientName), resultToAttacker);
                        sendSafely(nameToSocket.get(opponent), resultToDefender);
                    }

                    currentPlayer = opponent;
                    sendTurnInfo();
                    break;

                default:
                    break;
            }
        }
    }

    private void sendTurnInfo() {
        for (Map.Entry<String, WebSocket> entry : nameToSocket.entrySet()) {
            JSONObject msg = new JSONObject();
            if (entry.getKey().equals(currentPlayer)) {
                msg.put("type", "yourTurn");
            } else {
                msg.put("type", "opponentTurn");
            }
            sendSafely(entry.getValue(), msg);
        }
    }

    private void sendSafely(WebSocket socket, JSONObject msg) {
        try {
            socket.send(msg.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendClientsList() {
        JSONArray clientList = new JSONArray();
        for (String clientName : clients.values()) {
            clientList.put(clientName);
        }

        Iterator<Map.Entry<WebSocket, String>> iterator = clients.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<WebSocket, String> entry = iterator.next();
            WebSocket conn = entry.getKey();
            String clientName = entry.getValue();

            JSONObject rst = new JSONObject();
            rst.put("type", "clients");
            rst.put("id", clientName);
            rst.put("list", clientList);

            sendSafely(conn, rst);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server started on port: " + getPort());
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }

    public static void setSignTerm(Main server) {
        SignalHandler handler = sig -> {
            System.out.println(sig.getName() + " received. Stopping server...");
            try {
                server.stop(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Server stopped.");
            System.exit(0);
        };
        Signal.handle(new Signal("TERM"), handler);
        Signal.handle(new Signal("INT"), handler);
    }

    public static void main(String[] args) {
        int port = 3000;
        Main server = new Main(new InetSocketAddress(port));
        server.start();

        setSignTerm(server);

        LineReader reader = LineReaderBuilder.builder().build();
        System.out.println("Server running. Type 'exit' to gracefully stop it.");

        try {
            while (true) {
                String line;
                try {
                    line = reader.readLine("> ");
                } catch (UserInterruptException | EndOfFileException e) {
                    break;
                }

                line = line.trim();
                if (line.equalsIgnoreCase("exit")) {
                    System.out.println("Stopping server...");
                    server.stop(1000);
                    break;
                } else {
                    System.out.println("Unknown command. Type 'exit' to stop server gracefully.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("Server stopped.");
        }
    }
}
