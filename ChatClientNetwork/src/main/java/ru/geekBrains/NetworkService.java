package ru.geekBrains;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class NetworkService {
    private final Socket socket;
    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;

    public NetworkService(String address, int port, MessageService messageService) throws IOException {
        this.socket = new Socket(address, port);
        this.inputStream = new DataInputStream(socket.getInputStream());
        this.outputStream = new DataOutputStream(socket.getOutputStream());

        Thread t = new Thread(() -> {
            while (socket.isConnected()) {
                try {
                    String msg = inputStream.readUTF();
                    messageService.receiveMessage(msg);
                } catch (IOException e) {
                    try {
                        socket.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void writeMessage(String msg) throws IOException {
            outputStream.writeUTF(msg);
    }
}
