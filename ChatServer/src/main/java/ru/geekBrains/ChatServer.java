package ru.geekBrains;

import ru.geekBrains.auth.AuthService;
import ru.geekBrains.auth.DBAuthService;
import ru.geekbrains.messages.MessageDTO;
import ru.geekbrains.messages.MessageType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class ChatServer {
    private static final int PORT = 8189;
    private List<ClientHandler> onlineClientsList;
    private AuthService authService;

    public ChatServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)){
            System.out.println("Server started");
            authService = new DBAuthService();
            authService.start();
            onlineClientsList = new LinkedList<>();
            while (true) {
                System.out.println("Waiting for connection...");
                Socket socket = serverSocket.accept();
                System.out.println("Client connected!");
                new ClientHandler(socket, this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean isUserBusy(String userName){
        for (ClientHandler clientHandler : onlineClientsList) {
            if(clientHandler.getCurrentUserName().equals(userName)) return true;
        }
        return false;
    }

    public synchronized void broadcastMessage(MessageDTO dto) {
        for (ClientHandler clientHandler : onlineClientsList) {
            clientHandler.sendMessage(dto);
        }
    }

    public synchronized void sendPrivateMessage(MessageDTO dto){
        for (ClientHandler clientHandler : onlineClientsList) {
            if((clientHandler.getCurrentUserName()).equals(dto.getTo())){
                clientHandler.sendMessage(dto);
                break;
            }
        }
    }

    public synchronized void broadcastOnlineClients(){
        MessageDTO dto = new MessageDTO();
        dto.setMessageType(MessageType.CLIENTS_LIST_MESSAGE);

        List<String> isOnline = new LinkedList<>();
        for (ClientHandler clientHandler : onlineClientsList) {
            isOnline.add(clientHandler.getCurrentUserName());
        }
        dto.setUserOnline(isOnline);
        broadcastMessage(dto);
    }

    public synchronized void subscribe(ClientHandler c) {
        onlineClientsList.add(c);
        broadcastOnlineClients();
    }

    public synchronized void unsubscribe(ClientHandler c) {
        onlineClientsList.remove(c);
        broadcastOnlineClients();
    }

    public AuthService getAuthService() {
        return authService;
    }
}
