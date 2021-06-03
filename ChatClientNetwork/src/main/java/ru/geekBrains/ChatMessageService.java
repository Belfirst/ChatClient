package ru.geekBrains;

import java.io.IOException;

public class ChatMessageService implements MessageService{

    private final String HOST;
    private final int PORT;
    private NetworkService networkService;
    private final MessageProcessor PROCESSOR;

    public ChatMessageService(String host, int port, MessageProcessor processor) throws IOException {
        this.HOST = host;
        this.PORT = port;
        this.PROCESSOR = processor;
        connectToServer();
    }

    private void connectToServer() throws IOException {
            this.networkService = new NetworkService(HOST, PORT, this);
    }

    @Override
    public void sendMessage(String msg) throws IOException {
        networkService.writeMessage(msg);
    }

    @Override
    public void receiveMessage(String msg) {
        PROCESSOR.processMessage(msg);
    }
}
