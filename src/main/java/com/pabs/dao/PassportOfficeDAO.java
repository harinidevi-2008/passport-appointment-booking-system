package com.pabs.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.pabs.model.PassportOffice;
import com.pabs.util.DBConnection;

public class PassportOfficeDAO {

    private static final String SELECT_COLUMNS =
            "id, office_name, office_type, city, state, latitude, longitude, address, pincode, active, created_at";

    private static final String SELECT_ACTIVE_OFFICES =
            "SELECT " + SELECT_COLUMNS + " FROM passport_offices WHERE active=TRUE "
                    + "ORDER BY state, city, office_name";

    private static final String SELECT_ACTIVE_OFFICES_BY_CITY_STATE =
            "SELECT " + SELECT_COLUMNS + " FROM passport_offices "
                    + "WHERE active=TRUE AND city=? AND state=? ORDER BY office_name";

    private static final String SELECT_BY_ID =
            "SELECT " + SELECT_COLUMNS + " FROM passport_offices WHERE id=?";

    private static final String SELECT_DISTINCT_ACTIVE_LOCATIONS =
            "SELECT DISTINCT city, state FROM passport_offices WHERE active=TRUE ORDER BY state, city";

    public List<PassportOffice> findActiveOffices() {
        List<PassportOffice> offices = new ArrayList<>();

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(SELECT_ACTIVE_OFFICES);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                offices.add(mapOffice(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return offices;
    }

    public List<PassportOffice> findActiveOfficesByCityState(String city, String state) {
        List<PassportOffice> offices = new ArrayList<>();

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(SELECT_ACTIVE_OFFICES_BY_CITY_STATE)
        ) {

            ps.setString(1, city);
            ps.setString(2, state);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    offices.add(mapOffice(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return offices;
    }

    public PassportOffice findById(int officeId) {

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)
        ) {

            ps.setInt(1, officeId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapOffice(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<OfficeLocation> findDistinctActiveOfficeLocations() {
        List<OfficeLocation> locations = new ArrayList<>();

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement ps = connection.prepareStatement(SELECT_DISTINCT_ACTIVE_LOCATIONS);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                locations.add(new OfficeLocation(rs.getString("city"), rs.getString("state")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return locations;
    }

    private PassportOffice mapOffice(ResultSet rs) throws SQLException {
        PassportOffice office = new PassportOffice();
        office.setId(rs.getInt("id"));
        office.setOfficeName(rs.getString("office_name"));
        office.setOfficeType(rs.getString("office_type"));
        office.setCity(rs.getString("city"));
        office.setState(rs.getString("state"));
        office.setLatitude(rs.getBigDecimal("latitude"));
        office.setLongitude(rs.getBigDecimal("longitude"));
        office.setAddress(rs.getString("address"));
        office.setPincode(rs.getString("pincode"));
        office.setActive(rs.getBoolean("active"));
        office.setCreatedAt(rs.getTimestamp("created_at"));
        return office;
    }

    public static class OfficeLocation {
        private final String city;
        private final String state;

        public OfficeLocation(String city, String state) {
            this.city = city;
            this.state = state;
        }

        public String getCity() {
            return city;
        }

        public String getState() {
            return state;
        }
    }
}
