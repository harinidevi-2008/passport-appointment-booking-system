package com.pabs.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.pabs.model.Document;
import com.pabs.util.DBConnection;

public class DocumentDAO {

    private static final String SELECT_COLUMNS =
            "id, user_id, identity_proof, address_proof, photograph, created_at, updated_at";

    private static final String FIND_BY_USER_ID =
            "SELECT " + SELECT_COLUMNS + " FROM documents WHERE user_id=?";

    private static final String UPSERT_DOCUMENT =
            "INSERT INTO documents(user_id, identity_proof, address_proof, photograph) "
                    + "VALUES(?,?,?,?) "
                    + "ON DUPLICATE KEY UPDATE identity_proof=VALUES(identity_proof), "
                    + "address_proof=VALUES(address_proof), photograph=VALUES(photograph), "
                    + "updated_at=CURRENT_TIMESTAMP";

    private static final String FIND_ALL_WITH_USERS =
            "SELECT d.id, u.id AS user_id, d.identity_proof, d.address_proof, d.photograph, "
                    + "d.created_at, d.updated_at, u.full_name, u.email "
                    + "FROM users u LEFT JOIN documents d ON d.user_id=u.id "
                    + "WHERE UPPER(u.role)='USER' "
                    + "ORDER BY u.full_name, u.email";

    public Document findByUserId(int userId) {

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(FIND_BY_USER_ID)
        ) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapDocument(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean upsert(Document document) throws SQLException {

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(UPSERT_DOCUMENT)
        ) {

            ps.setInt(1, document.getUserId());
            ps.setString(2, document.getIdentityProof());
            ps.setString(3, document.getAddressProof());
            ps.setString(4, document.getPhotograph());

            return ps.executeUpdate() > 0;

        }
    }

    public List<Document> findAllWithUsers() {
        List<Document> documents = new ArrayList<>();

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(FIND_ALL_WITH_USERS);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                Document document = mapDocument(rs);
                document.setUserFullName(rs.getString("full_name"));
                document.setUserEmail(rs.getString("email"));
                documents.add(document);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return documents;
    }

    private Document mapDocument(ResultSet rs) throws SQLException {
        Document document = new Document();
        document.setId(rs.getInt("id"));
        document.setUserId(rs.getInt("user_id"));
        document.setIdentityProof(rs.getString("identity_proof"));
        document.setAddressProof(rs.getString("address_proof"));
        document.setPhotograph(rs.getString("photograph"));
        document.setCreatedAt(rs.getTimestamp("created_at"));
        document.setUpdatedAt(rs.getTimestamp("updated_at"));
        return document;
    }
}
