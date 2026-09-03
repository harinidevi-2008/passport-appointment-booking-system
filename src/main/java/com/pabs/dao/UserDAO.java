package com.pabs.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.pabs.model.User;
import com.pabs.util.DBConnection;
import com.pabs.util.PasswordUtil;

public class UserDAO {

    private static final String INSERT_USER =
            "INSERT INTO users(full_name,email,password,role) VALUES(?,?,?,?)";

    private static final String LOGIN_USER =
            "SELECT id, full_name, email, password, role, created_at FROM users WHERE email=?";

    private static final String FIND_USER_BY_EMAIL =
            "SELECT id, full_name, email, password, role, created_at FROM users WHERE email=?";

    private static final String FIND_USER_BY_ID =
            "SELECT id, full_name, email, password, role, created_at FROM users WHERE id=?";

    private static final String FIND_USER_BY_EMAIL_EXCLUDING_ID =
            "SELECT id, full_name, email, password, role, created_at FROM users WHERE email=? AND id<>?";

    private static final String UPDATE_PASSWORD =
            "UPDATE users SET password=? WHERE id=?";

    private static final String UPDATE_PROFILE =
            "UPDATE users SET full_name=?, email=? WHERE id=? AND role='USER'";

    // Register User
    public boolean registerUser(User user) {

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(INSERT_USER)
        ) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, PasswordUtil.hashPassword(user.getPassword()));
            ps.setString(4, "USER");

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // Login User
    public User loginUser(String email, String password) {

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(LOGIN_USER)
        ) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                if (PasswordUtil.verifyPassword(password, storedPassword)) {
                    return mapUser(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public User findByEmail(String email) {

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(FIND_USER_BY_EMAIL)
        ) {

            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapUser(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public User findById(int userId) {

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(FIND_USER_BY_ID)
        ) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapUser(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean emailExistsForAnotherUser(String email, int userId) {

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(FIND_USER_BY_EMAIL_EXCLUDING_ID)
        ) {

            ps.setString(1, email);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return true;
    }

    public boolean updatePassword(int userId, String password) {

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(UPDATE_PASSWORD)
        ) {

            ps.setString(1, PasswordUtil.hashPassword(password));
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateProfile(int userId, String fullName, String email) {

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(UPDATE_PROFILE)
        ) {

            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setInt(3, userId);

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();

        user.setId(rs.getInt("id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getString("role"));
        user.setCreatedAt(rs.getTimestamp("created_at"));

        return user;
    }
}
