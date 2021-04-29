package ru.geekBrains;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.geekBrains.db.DBService;
import ru.geekbrains.messages.MessageDTO;
import ru.geekbrains.messages.MessageType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler {
    private Socket socket;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;
    private ChatServer chatServer;
    private String currentUserName;

    static final Logger userLogger = LogManager.getLogger(ClientHandler.class.getName());

    public ClientHandler(Socket socket, ChatServer chatServer) {
        try {
            this.chatServer = chatServer;
            this.socket = socket;
            this.inputStream = new DataInputStream(socket.getInputStream());
            this.outputStream = new DataOutputStream(socket.getOutputStream());
            userLogger.info("CH created!");

            ExecutorService es = Executors.newCachedThreadPool();
            es.execute(() -> {
                if(authenticate())
                    readMessages();
            });
            es.shutdown();

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
            while (!Thread.currentThread().isInterrupted() || socket.isConnected()) {
                String msg = inputStream.readUTF();
                MessageDTO dto = MessageDTO.convertFromJson(msg);
                dto.setFrom(currentUserName);
                userLogger.info(currentUserName + " send message");

                switch (dto.getMessageType()) {
                    case PUBLIC_MESSAGE -> chatServer.broadcastMessage(dto);
                    case PRIVATE_MESSAGE -> chatServer.sendPrivateMessage(dto);
                    case SERVICE_MESSAGE -> {
                        currentUserName = replaceUserName(currentUserName, dto.getBody());
                        chatServer.broadcastOnlineClients();
                        chatServer.broadcastMessage(dto);

                    }

                }
            }
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            userLogger.info(currentUserName + " disconnect");
            throw new RuntimeException("Disconnect",e);
        } finally {
            closeHandler();
        }
    }

    public String getCurrentUserName() {
        return currentUserName;
    }

    private boolean authenticate() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                userLogger.info("Disconnect client");
                closeHandler();
            }
        },30000);
        userLogger.info("Authenticate started!");
        try {
            while (true) {
                String authMessage = inputStream.readUTF();
                userLogger.info("Received msg");
                MessageDTO dto = MessageDTO.convertFromJson(authMessage);
                String username = chatServer.getAuthService().getUsernameByLoginPass(dto.getLogin(), dto.getPassword());
                MessageDTO response = new MessageDTO();
                if (username == null) {
                    response.setMessageType(MessageType.ERROR_MESSAGE);
                    response.setBody("Wrong login or pass!");
                    userLogger.info("Wrong auth");
                } else if(chatServer.isUserBusy(username)) {
                    response.setMessageType(MessageType.ERROR_MESSAGE);
                    response.setBody("U're clone");
                    System.out.println("Clone");
                    userLogger.info("duplicate client");
                } else {
                    response.setMessageType(MessageType.AUTH_CONFIRM);
                    response.setBody(username);
                    response.setLogin(dto.getLogin());
                    currentUserName = username;
                    chatServer.subscribe(this);
                    userLogger.info(currentUserName + " Subscribed");
                    sendMessage(response);
                    timer.cancel();
                    return true;
                }
                sendMessage(response);
            }
        } catch (IOException e) {
            closeHandler();
            throw new RuntimeException("Error auth", e);
        }

    }

    public String replaceUserName(String userName, String newUserName) {
        DBService dbService = new DBService();
        if(!dbService.changeUserName(userName,newUserName)) return userName;
        userLogger.info(userName + " change " + newUserName);
        return newUserName;
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
