package ru.geekBrains;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(65500)) {
            System.out.println("Server started!");
            Socket socket = serverSocket.accept();
            System.out.println("Client connected!");
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            while (true) {

                String message = in.readUTF();
                System.out.println("Received " + message);
                out.writeUTF("Echo: " + message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
