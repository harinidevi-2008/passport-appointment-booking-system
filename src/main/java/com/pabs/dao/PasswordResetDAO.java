package com.pabs.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import com.pabs.util.DBConnection;

public class PasswordResetDAO {

    private static final String CREATE_OTP =
            "INSERT INTO password_reset_otp(user_id, otp_hash, expires_at) VALUES(?,?,?)";

    private static final String FIND_OTP =
            "SELECT id, user_id, otp_hash, expires_at, attempts, used, created_at "
                    + "FROM password_reset_otp WHERE id=? AND user_id=?";

    private static final String INCREMENT_ATTEMPTS =
            "UPDATE password_reset_otp SET attempts=attempts+1 WHERE id=?";

    private static final String MARK_USED =
            "UPDATE password_reset_otp SET used=TRUE WHERE id=?";

    private static final String INVALIDATE_ACTIVE_FOR_USER =
            "UPDATE password_reset_otp SET used=TRUE WHERE user_id=? AND used=FALSE";

    public int createOtp(int userId, String otpHash, LocalDateTime expiresAt) {

        invalidateActiveOtpsForUser(userId);

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(CREATE_OTP, Statement.RETURN_GENERATED_KEYS)
        ) {

            ps.setInt(1, userId);
            ps.setString(2, otpHash);
            ps.setTimestamp(3, Timestamp.valueOf(expiresAt));

            if (ps.executeUpdate() == 0) {
                return 0;
            }

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public Optional<PasswordResetOtpRecord> findOtp(int otpId, int userId) {

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(FIND_OTP)
        ) {

            ps.setInt(1, otpId);
            ps.setInt(2, userId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(new PasswordResetOtpRecord(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("otp_hash"),
                        rs.getTimestamp("expires_at").toLocalDateTime(),
                        rs.getInt("attempts"),
                        rs.getBoolean("used")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public void incrementAttempts(int otpId) {

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(INCREMENT_ATTEMPTS)
        ) {

            ps.setInt(1, otpId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void markUsed(int otpId) {

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(MARK_USED)
        ) {

            ps.setInt(1, otpId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void invalidateActiveOtpsForUser(int userId) {

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(INVALIDATE_ACTIVE_FOR_USER)
        ) {

            ps.setInt(1, userId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static class PasswordResetOtpRecord {
        private final int id;
        private final int userId;
        private final String otpHash;
        private final LocalDateTime expiresAt;
        private final int attempts;
        private final boolean used;

        public PasswordResetOtpRecord(int id, int userId, String otpHash,
                                      LocalDateTime expiresAt, int attempts, boolean used) {
            this.id = id;
            this.userId = userId;
            this.otpHash = otpHash;
            this.expiresAt = expiresAt;
            this.attempts = attempts;
            this.used = used;
        }

        public int getId() {
            return id;
        }

        public int getUserId() {
            return userId;
        }

        public String getOtpHash() {
            return otpHash;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }

        public int getAttempts() {
            return attempts;
        }

        public boolean isUsed() {
            return used;
        }
    }
}
