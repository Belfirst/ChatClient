package ru.geekBrains.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PrimitiveAuthService implements AuthService {
    private List<Client> clients;

    public PrimitiveAuthService() {
        clients = new ArrayList<>(Arrays.asList(
                new Client("user1", "log1", "pass1"),
                new Client("user2", "log2", "pass2"),
                new Client("user3", "log3", "pass3")
        ));
    }

    @Override
    public String getUsernameByLoginPass(String login, String pass) {
        for (Client c : clients) {
            if (c.getLogin().equals(login) && c.getPassword().equals(pass)) return c.getUsername();
        }
        return null;
    }
}
