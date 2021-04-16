package ru.geekBrains;

import java.sql.*;

public class DBService {
    private Connection connection;
    private PreparedStatement psSelect;
    private PreparedStatement psUpdate;

    public void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:mainChat.db");
    }

    public String getUsername(String login, String pass){
        try {
            connect();
            psSelect = connection.prepareStatement(
                    "SELECT username FROM clients WHERE login = ? AND password = ?;");
            psSelect.setString(1,login);
            psSelect.setString(2, pass);
            ResultSet rs = psSelect.executeQuery();

            while (rs.next()){
                String name = rs.getString("username");
                if(name != null) return name;
            }

            rs.close();
        } catch (SQLException | ClassNotFoundException throwables) {
            throwables.printStackTrace();
        } finally {
            disconnect(psSelect);
        }
        return null;
    }

    public boolean changeUserName(String userName, String newUserName) {
        try {
            connect();
            psUpdate = connection.prepareStatement(
                    "UPDATE clients SET username = ? WHERE username = ?;");
            psUpdate.setString(1,newUserName);
            psUpdate.setString(2,userName);
            psUpdate.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            disconnect(psUpdate);
        }
        return true;
    }

    public void disconnect(PreparedStatement ps) {
        try {
            if (ps != null) {
                ps.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
