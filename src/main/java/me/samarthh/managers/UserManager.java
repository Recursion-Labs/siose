package me.samarthh.managers;

import java.sql.*;
import java.util.UUID;

public class UserManager {
    private Connection connection;

    public UserManager() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:users.db");
            createTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "uuid TEXT PRIMARY KEY," +
                "token TEXT," +
                "authenticated INTEGER DEFAULT 0" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public boolean isAuthenticated(UUID uuid) {
        String sql = "SELECT authenticated FROM users WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("authenticated") == 1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getToken(UUID uuid) {
        String sql = "SELECT token FROM users WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("token");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setToken(UUID uuid, String token) {
        String sql = "INSERT OR REPLACE INTO users (uuid, token, authenticated) VALUES (?, ?, 1)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, token);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}