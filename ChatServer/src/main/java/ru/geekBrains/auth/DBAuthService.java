package ru.geekBrains.auth;

import ru.geekBrains.db.DBService;

public class DBAuthService implements AuthService {

    @Override
    public String getUsernameByLoginPass(String login, String pass) {

        DBService db = new DBService();
        return db.getUsername(login,pass);

    }
}
