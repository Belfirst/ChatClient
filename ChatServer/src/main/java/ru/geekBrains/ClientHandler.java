package ru.geekBrains;

import ru.geekbrains.messages.MessageDTO;
import ru.geekbrains.messages.MessageType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Timer;

public class ClientHandler {
    private Socket socket;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;
    private ChatServer chatServer;
    private String currentUserName;

    public ClientHandler(Socket socket, ChatServer chatServer) {
        try {
            this.chatServer = chatServer;
            this.socket = socket;
            this.inputStream = new DataInputStream(socket.getInputStream());
            this.outputStream = new DataOutputStream(socket.getOutputStream());
            System.out.println("CH created!");

            new Thread(() -> {
                authenticate();
                readMessages();
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(MessageDTO dto) {
        try {
            outputStream.writeUTF(dto.convertToJson());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readMessages() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String msg = inputStream.readUTF();
                MessageDTO dto = MessageDTO.convertFromJson(msg);
                dto.setFrom(currentUserName);

                switch (dto.getMessageType()) {
                    case PUBLIC_MESSAGE -> chatServer.broadcastMessage(dto);
                    case PRIVATE_MESSAGE -> chatServer.sendPrivateMessage(dto);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        } finally {
            closeHandler();
        }
    }

    public String getCurrentUserName() {
        return currentUserName;
    }

    private void authenticate() {
        System.out.println("Authenticate started!");
        try {
            while (true) {
                String authMessage = inputStream.readUTF();
                System.out.println("received msg ");
                MessageDTO dto = MessageDTO.convertFromJson(authMessage);
                String username = chatServer.getAuthService().getUsernameByLoginPass(dto.getLogin(), dto.getPassword());
                MessageDTO response = new MessageDTO();
                if (username == null) {
                    response.setMessageType(MessageType.ERROR_MESSAGE);
                    response.setBody("Wrong login or pass!");
                    System.out.println("Wrong auth");
                } else if(chatServer.isUserBusy(username)) {
                    response.setMessageType(MessageType.ERROR_MESSAGE);
                    response.setBody("U're clone");
                    System.out.println("Clone");
                } else {
                    response.setMessageType(MessageType.AUTH_CONFIRM);
                    response.setBody(username);
                    currentUserName = username;
                    chatServer.subscribe(this);
                    System.out.println("Subscribed");
                    sendMessage(response);
                    break;
                }
                sendMessage(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
            closeHandler();
        }
    }

    public void closeHandler() {
        try {
            chatServer.unsubscribe(this);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
