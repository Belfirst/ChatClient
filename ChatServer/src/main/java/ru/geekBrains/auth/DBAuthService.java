package ru.geekBrains.auth;

import ru.geekBrains.DBService;

public class DBAuthService implements AuthService {

    @Override
    public void start() {
        System.out.println("Auth started");
    }

    @Override
    public void stop() {
        System.out.println("Auth stopped");
    }

    @Override
    public String getUsernameByLoginPass(String login, String pass) {

        DBService db = new DBService();
        return db.getUsername(login,pass);

    }
}
