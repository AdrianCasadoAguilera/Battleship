package com.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import com.client.controllers.GameController;
import com.client.controllers.LobbyController;

import javafx.application.Platform;

public class UtilsWS {

    private static UtilsWS sharedInstance = null;
    private WebSocketClient client;
    private Consumer<String> onOpenCallBack = null;
    private Consumer<String> onMessageCallBack = null;
    private Consumer<String> onCloseCallBack = null;
    private Consumer<String> onErrorCallBack = null;
    private String location = "";
    private static AtomicBoolean exitRequested = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private String clientName = null;

    private UtilsWS(String location, String name) {
        this.location = location;
        createNewWebSocketClient(name);
    }

    private void createNewWebSocketClient(String name) {
        try {
            this.client = new WebSocketClient(new URI(location), new Draft_6455()) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    String message = "WS connected to: " + getURI();
                    System.out.println(message);

                    clientName = name;
                    client.send("{\"type\":\"register\",\"name\":\"" + name + "\"}");

                    if (onOpenCallBack != null) {
                        onOpenCallBack.accept(message);
                    }
                }

                @Override
                public void onMessage(String message) {
                    if (onMessageCallBack != null) {
                        onMessageCallBack.accept(message);
                    }
                    System.out.println("WS message: " + message);
                    JSONObject obj = new JSONObject(message);
                    if (obj.has("type")) {
                        String type = obj.getString("type");
                        switch (type) {
                            case "starting":
                                System.out.println("Received starting message");
                                LobbyController lobbyController = (LobbyController) UtilsViews.getController("lobby");
                                lobbyController.startWaiting();
                                break;
                            case "start":
                                System.out.println("Game started");
                                Platform.runLater(() -> {
                                    UtilsViews.setView("choose");
                                });
                                break;
                            case "startGame":
                                System.out.println("Game started");
                                Platform.runLater(() -> {
                                    UtilsViews.setView("game");
                                    GameController gameController = (GameController) UtilsViews.getController("game");
                                    gameController.initialize();
                                });
                                break;
                            case "attackResult":
                                Platform.runLater(() -> {
                                    GameController controller = (GameController) UtilsViews.getController("game");
                                    int row = obj.getInt("row");
                                    int col = obj.getInt("col");
                                    boolean hit = obj.getBoolean("hit");
                                    controller.handleAttackResult(row, col, hit);
                                });
                                break;
                            case "gotAttacked":
                                Platform.runLater(() -> {
                                    GameController controller = (GameController) UtilsViews.getController("game");
                                    int row = obj.getInt("row");
                                    int col = obj.getInt("col");
                                    boolean hit = obj.getBoolean("hit");
                                    controller.handleGotAttacked(row, col, hit);
                                });
                                break;
                            default:
                                System.out.println("WS unknown message type: " + type);
                        }
                    } else {
                        System.out.println("WS unknown message format: " + message);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    String message = "WS closed connection from: " + getURI() + " with reason: " + reason;
                    System.out.println(message);
                    if (onCloseCallBack != null) {
                        onCloseCallBack.accept(message);
                    }
                    if (remote) {
                        scheduleReconnect();
                    }
                }

                @Override
                public void onError(Exception e) {
                    String message = "WS connection error: " + e.getMessage();
                    System.out.println(message);
                    if (onErrorCallBack != null) {
                        onErrorCallBack.accept(message);
                    }
                    if (e.getMessage().contains("Connection refused") || e.getMessage().contains("Connection reset")) {
                        scheduleReconnect();
                    }
                }
            };
            this.client.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            System.out.println("WS Error, " + location + " is not a valid URI");
        }
    }

    private void scheduleReconnect() {
        if (!exitRequested.get()) {
            scheduler.schedule(this::reconnect, 5, TimeUnit.SECONDS);
        }
    }

    private void reconnect() {
        if (exitRequested.get()) {
            return;
        }

        System.out.println("WS reconnecting to: " + this.location);

        if (client != null) {
            client.close();
        }
        createNewWebSocketClient(clientName);
    }

    public static UtilsWS getSharedInstance(String location, String name) {
        if (sharedInstance == null) {
            sharedInstance = new UtilsWS(location, name);
        }
        return sharedInstance;
    }

    public void onOpen(Consumer<String> callBack) {
        this.onOpenCallBack = callBack;
    }

    public void onMessage(Consumer<String> callBack) {
        this.onMessageCallBack = callBack;
    }

    public void onClose(Consumer<String> callBack) {
        this.onCloseCallBack = callBack;
    }

    public void onError(Consumer<String> callBack) {
        this.onErrorCallBack = callBack;
    }

    public void safeSend(String text) {
        try {
            if (client != null && client.isOpen()) {
                client.send(text);
            } else {
                System.out.println("WS Error: Client is not connected. Attempting to reconnect...");
                scheduleReconnect();
            }
        } catch (Exception e) {
            System.out.println("WS Error sending message: " + e.getMessage());
        }
    }

    public void forceExit() {
        System.out.println("WS Closing ...");
        exitRequested.set(true);
        try {
            if (client != null && !client.isClosed()) {
                client.closeBlocking();
            }
        } catch (Exception e) {
            System.out.println("WS Interrupted while closing WebSocket connection: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            scheduler.shutdownNow();
        }
    }

    public boolean isOpen() {
        return client != null && client.isOpen();
    }
}