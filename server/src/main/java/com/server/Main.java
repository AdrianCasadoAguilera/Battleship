package com.server;
import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;

public class Main extends WebSocketServer {

    private static String host = "192.168.82.143";
    private static int port = 3000;

    public Main(InetSocketAddress address){
        super(address);
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
    public void onClose(WebSocket arg0, int arg1, String arg2, boolean arg3) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onClose'");
    }

    @Override
    public void onError(WebSocket arg0, Exception arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onError'");
    }

    @Override
    public void onMessage(WebSocket arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onMessage'");
    }

    @Override
    public void onOpen(WebSocket client, ClientHandshake arg1) {
        System.out.println("New client connected: "+client);
    }

    @Override
    public void onStart() {
        System.out.println("Server started at port "+port);
        setConnectionLostTimeout(1000);
    }
}