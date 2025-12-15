package com.faceRecogntion.FaceRecognition;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;

public class DBHelper {
    private static final String URL = "jdbc:mysql://localhost:3306/yourdb";
    private static final String USER = "root";
    private static final String PASSWORD = "password";

    public static void insertAttendance(int personId, String name) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();

            // Check if already marked
            String checkSQL = "SELECT * FROM attendance WHERE person_id=? AND date=?";
            try (PreparedStatement ps = conn.prepareStatement(checkSQL)) {
                ps.setInt(1, personId);
                ps.setDate(2, Date.valueOf(today));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    System.out.println(name + " already marked today.");
                    return; // skip insert
                }
            }

            // Insert attendance
            String insertSQL = "INSERT INTO attendance(person_id, name, date, time) VALUES(?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSQL)) {
                ps.setInt(1, personId);
                ps.setString(2, name);
                ps.setDate(3, Date.valueOf(today));
                ps.setTime(4, Time.valueOf(now));
                ps.executeUpdate();
                System.out.println("Attendance marked for " + name);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
