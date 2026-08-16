package com.pabs.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

import com.pabs.model.PassportApplication;
import com.pabs.util.DBConnection;

public class PassportApplicationDAO {

    private static final String INSERT_APPLICATION =
            "INSERT INTO passport_applications(application_number, user_id, application_type, passport_mode, "
                    + "full_name, date_of_birth, gender, place_of_birth, father_name, mother_name, phone, email, "
                    + "address, city, state, pincode, status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String UPDATE_APPLICATION_NUMBER =
            "UPDATE passport_applications SET application_number=? WHERE id=?";

    private static final String SELECT_BY_ID =
            "SELECT id, application_number, user_id, application_type, passport_mode, full_name, date_of_birth, "
                    + "gender, place_of_birth, father_name, mother_name, phone, email, address, city, state, "
                    + "pincode, status, review_note, created_at, updated_at FROM passport_applications WHERE id=?";

    private static final String SELECT_BY_USER_ID =
            "SELECT id, application_number, user_id, application_type, passport_mode, full_name, date_of_birth, "
                    + "gender, place_of_birth, father_name, mother_name, phone, email, address, city, state, "
                    + "pincode, status, review_note, created_at, updated_at FROM passport_applications WHERE user_id=? "
                    + "ORDER BY created_at DESC";

    private static final String SELECT_ALL =
            "SELECT id, application_number, user_id, application_type, passport_mode, full_name, date_of_birth, "
                    + "gender, place_of_birth, father_name, mother_name, phone, email, address, city, state, "
                    + "pincode, status, review_note, created_at, updated_at FROM passport_applications "
                    + "ORDER BY created_at DESC";

    private static final String UPDATE_STATUS =
            "UPDATE passport_applications SET status=?, review_note=? WHERE id=? AND status=?";

    public boolean createApplication(PassportApplication application) {
        Connection connection = null;

        try {
            connection = DBConnection.getConnection();
            if (connection == null) {
                return false;
            }

            connection.setAutoCommit(false);
            String pendingNumber = "PABS-PENDING-" + System.nanoTime();

            try (PreparedStatement ps = connection.prepareStatement(INSERT_APPLICATION, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, pendingNumber);
                ps.setInt(2, application.getUserId());
                ps.setString(3, application.getApplicationType());
                ps.setString(4, application.getPassportMode());
                ps.setString(5, application.getFullName());
                ps.setDate(6, Date.valueOf(application.getDateOfBirth()));
                ps.setString(7, application.getGender());
                ps.setString(8, application.getPlaceOfBirth());
                ps.setString(9, application.getFatherName());
                ps.setString(10, application.getMotherName());
                ps.setString(11, application.getPhone());
                ps.setString(12, application.getEmail());
                ps.setString(13, application.getAddress());
                ps.setString(14, application.getCity());
                ps.setString(15, application.getState());
                ps.setString(16, application.getPincode());
                ps.setString(17, "SUBMITTED");

                if (ps.executeUpdate() == 0) {
                    connection.rollback();
                    return false;
                }

                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (!generatedKeys.next()) {
                        connection.rollback();
                        return false;
                    }

                    int id = generatedKeys.getInt(1);
                    String applicationNumber = generateApplicationNumber(id);

                    try (PreparedStatement updatePs = connection.prepareStatement(UPDATE_APPLICATION_NUMBER)) {
                        updatePs.setString(1, applicationNumber);
                        updatePs.setInt(2, id);

                        if (updatePs.executeUpdate() == 0) {
                            connection.rollback();
                            return false;
                        }
                    }

                    application.setId(id);
                    application.setApplicationNumber(applicationNumber);
                    application.setStatus("SUBMITTED");
                }
            }

            connection.commit();
            return true;

        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
        } finally {
            closeQuietly(connection);
        }

        return false;
    }

    public PassportApplication getApplicationById(int id) {
        try (Connection connection = DBConnection.getConnection()) {
            if (connection == null) {
                return null;
            }

            try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapApplication(rs);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<PassportApplication> getApplicationsByUserId(int userId) {
        List<PassportApplication> applications = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection()) {
            if (connection == null) {
                return applications;
            }

            try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_USER_ID)) {
                ps.setInt(1, userId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        applications.add(mapApplication(rs));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return applications;
    }

    public List<PassportApplication> getAllApplications() {
        List<PassportApplication> applications = new ArrayList<>();

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(SELECT_ALL);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                applications.add(mapApplication(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return applications;
    }

    public boolean updateApplicationStatus(int applicationId, String currentStatus, String status, String reviewNote) {

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(UPDATE_STATUS)
        ) {

            ps.setString(1, status);
            ps.setString(2, reviewNote);
            ps.setInt(3, applicationId);
            ps.setString(4, currentStatus);

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    private String generateApplicationNumber(int id) {
        return String.format("PABS-%d-%05d", Year.now().getValue(), id);
    }

    private PassportApplication mapApplication(ResultSet rs) throws SQLException {
        PassportApplication application = new PassportApplication();
        application.setId(rs.getInt("id"));
        application.setApplicationNumber(rs.getString("application_number"));
        application.setUserId(rs.getInt("user_id"));
        application.setApplicationType(rs.getString("application_type"));
        application.setPassportMode(rs.getString("passport_mode"));
        application.setFullName(rs.getString("full_name"));
        application.setDateOfBirth(rs.getDate("date_of_birth").toLocalDate());
        application.setGender(rs.getString("gender"));
        application.setPlaceOfBirth(rs.getString("place_of_birth"));
        application.setFatherName(rs.getString("father_name"));
        application.setMotherName(rs.getString("mother_name"));
        application.setPhone(rs.getString("phone"));
        application.setEmail(rs.getString("email"));
        application.setAddress(rs.getString("address"));
        application.setCity(rs.getString("city"));
        application.setState(rs.getString("state"));
        application.setPincode(rs.getString("pincode"));
        application.setStatus(rs.getString("status"));
        application.setReviewNote(rs.getString("review_note"));
        application.setCreatedAt(rs.getTimestamp("created_at"));
        application.setUpdatedAt(rs.getTimestamp("updated_at"));
        return application;
    }

    private void rollbackQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.setAutoCommit(true);
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
