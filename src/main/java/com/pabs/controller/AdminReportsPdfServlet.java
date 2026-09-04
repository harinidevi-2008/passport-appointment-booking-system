package com.pabs.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pabs.dao.AdminReportDAO;
import com.pabs.dao.AdminReportDAO.AdminReportData;
import com.pabs.dao.AdminReportDAO.DailyAppointmentReportRow;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/admin-reports/pdf")
public class AdminReportsPdfServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AdminReportsPdfServlet.class.getName());
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ISO_DATE;
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final AdminReportDAO reportDAO = new AdminReportDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("../login.jsp");
            return;
        }
        if (!"ADMIN".equalsIgnoreCase((String) session.getAttribute("role"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        LocalDate selectedDate = parseDate(request.getParameter("date"));
        if (selectedDate == null) {
            selectedDate = LocalDate.now();
        }

        AdminReportData reportData = reportDAO.loadReportData(
                selectedDate,
                AdminReportsServlet.APPLICATION_STATUSES,
                AdminReportsServlet.APPOINTMENT_STATUSES);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"PABS_Admin_Report_" + selectedDate.format(FILE_DATE) + ".pdf\"");

        try {
            writePdf(reportData, response);
        } catch (DocumentException e) {
            if (!response.isCommitted()) {
                response.reset();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Unable to generate the report. Please try again later.");
            }
            LOGGER.log(Level.SEVERE, "Admin report PDF generation failed.", e);
        }
    }

    private void writePdf(AdminReportData data, HttpServletResponse response)
            throws IOException, DocumentException {
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        document.add(new Paragraph("Passport Appointment Booking System - Administrative Report", titleFont));
        document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DISPLAY_DATE_TIME)));
        document.add(new Paragraph("Daily appointment date: " + data.getSelectedDate()));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Summary", sectionFont));
        PdfPTable summary = table(4);
        addHeader(summary, "Metric");
        addHeader(summary, "Count");
        addHeader(summary, "Metric");
        addHeader(summary, "Count");
        addPair(summary, "Total Citizens", data.getTotalCitizens());
        addPair(summary, "Total Applications", data.getTotalApplications());
        addPair(summary, "Verified Applications", data.getVerifiedApplications());
        addPair(summary, "Processing Applications", data.getProcessingApplications());
        addPair(summary, "Total Appointments", data.getTotalAppointments());
        addPair(summary, "Completed Appointments", data.getCompletedAppointments());
        addPair(summary, "Cancelled Appointments", data.getCancelledAppointments());
        addPair(summary, "NO_SHOW Appointments", data.getNoShowAppointments());
        document.add(summary);
        document.add(new Paragraph(" "));

        addStatusTable(document, "Application Status Summary", data.getApplicationSummary(), sectionFont);
        addStatusTable(document, "Appointment Status Summary", data.getAppointmentSummary(), sectionFont);

        document.add(new Paragraph("Daily Appointment Report", sectionFont));
        PdfPTable appointments = table(8);
        appointments.setWidths(new float[] {1.4f, 1.4f, 2f, 2.4f, 2.6f, 1.2f, 1.2f, 1.5f});
        addHeader(appointments, "Appointment ID");
        addHeader(appointments, "Application ID");
        addHeader(appointments, "Citizen");
        addHeader(appointments, "Office");
        addHeader(appointments, "Date");
        addHeader(appointments, "Start");
        addHeader(appointments, "End");
        addHeader(appointments, "Status");
        for (DailyAppointmentReportRow row : data.getDailyAppointments()) {
            addCell(appointments, row.getAppointmentNumber());
            addCell(appointments, row.getApplicationNumber());
            addCell(appointments, row.getCitizenName());
            addCell(appointments, row.getOfficeName());
            addCell(appointments, row.getAppointmentDate());
            addCell(appointments, row.getStartTime());
            addCell(appointments, row.getEndTime());
            addCell(appointments, row.getAppointmentStatus());
        }
        document.add(appointments);
        document.close();
    }

    private void addStatusTable(Document document, String title, Map<String, Integer> data, Font sectionFont)
            throws DocumentException {
        document.add(new Paragraph(title, sectionFont));
        PdfPTable table = table(2);
        addHeader(table, "Status");
        addHeader(table, "Count");
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            addCell(table, entry.getKey());
            addCell(table, entry.getValue());
        }
        document.add(table);
        document.add(new Paragraph(" "));
    }

    private PdfPTable table(int columns) {
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSpacingBefore(6);
        table.setSpacingAfter(6);
        return table;
    }

    private void addPair(PdfPTable table, String label, int value) {
        addCell(table, label);
        addCell(table, value);
    }

    private void addHeader(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, Object value) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : String.valueOf(value),
                FontFactory.getFont(FontFactory.HELVETICA, 9)));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private LocalDate parseDate(String value) {
        try {
            return value == null || value.trim().isEmpty() ? null : LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
