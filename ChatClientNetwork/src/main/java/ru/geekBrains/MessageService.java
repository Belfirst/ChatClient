package ru.geekBrains;

import java.io.IOException;

public interface MessageService {
    void sendMessage(String msg) throws IOException;
    void receiveMessage(String msg);
}
