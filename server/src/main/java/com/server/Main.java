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
                    System.out.println("Client registered: " + name);
                    sendClientsList();

                    if(clients.size() == 2) {
                        JSONObject rst = new JSONObject();
                        rst.put("type", "starting");
                        rst.put("message", "5s to start");

                        for (Map.Entry<WebSocket, String> entry : clients.entrySet()) {
                            try {
                                entry.getKey().send(rst.toString());
                            } catch (WebsocketNotConnectedException e) {
                                System.out.println("Client " + entry.getValue() + " not connected.");
                                clients.remove(entry.getKey());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        rst.clear();

                        ScheduledExecutorService timeout = Executors.newSingleThreadScheduledExecutor();

                        timeout.schedule(() -> {
                            rst.put("type", "start");
                            rst.put("message", "Game started");
                            for (Map.Entry<WebSocket, String> entry : clients.entrySet()) {
                                try {
                                    entry.getKey().send(rst.toString());
                                } catch (WebsocketNotConnectedException e) {
                                    System.out.println("Client " + entry.getValue() + " not connected.");
                                    clients.remove(entry.getKey());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }, 5, TimeUnit.SECONDS);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    // Envia un mensaje a todos los clientes conectados
    private void broadcastMessage(String message, WebSocket sender) {
        for (Map.Entry<WebSocket, String> entry : clients.entrySet()) {
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

    // Envia un mensaje al cliente especificado
    private void sendPrivateMessage(String destination, String message, WebSocket senderConn) {
        boolean found = false;

        for (Map.Entry<WebSocket, String> entry : clients.entrySet()) {
            if (entry.getValue().equals(destination)) {
                found = true;
                try {
                    entry.getKey().send(message);
                    JSONObject confirmation = new JSONObject();
                    confirmation.put("type", "confirmation");
                    confirmation.put("message", "Message sent to " + destination);
                    senderConn.send(confirmation.toString());
                } catch (WebsocketNotConnectedException e) {
                    System.out.println("Client " + destination + " not connected.");
                    clients.remove(entry.getKey());
                    notifySenderClientUnavailable(senderConn, destination);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }

        if (!found) {
            System.out.println("Client " + destination + " not found.");
            notifySenderClientUnavailable(senderConn, destination);
        }
    }

    // Envia error de cliente no conectado
    private void notifySenderClientUnavailable(WebSocket sender, String destination) {
        JSONObject rst = new JSONObject();
        rst.put("type", "error");
        rst.put("message", "Client " + destination + " not available.");

        try {
            sender.send(rst.toString());
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

            try {
                conn.send(rst.toString());
            } catch (WebsocketNotConnectedException e) {
                System.out.println("Client " + clientName + " not connected.");
                iterator.remove();
            } catch (Exception e) {
                e.printStackTrace();
            }
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

        // Permet aturar el servidor amb SITERM/SIGINT
        setSignTerm(server);

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
}