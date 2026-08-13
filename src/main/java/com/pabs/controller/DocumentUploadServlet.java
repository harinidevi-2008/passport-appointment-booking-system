package com.pabs.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pabs.dao.DocumentDAO;
import com.pabs.model.Document;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

@WebServlet({"/documents", "/admin-documents"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 5 * 1024 * 1024,
        maxRequestSize = 12 * 1024 * 1024
)
public class DocumentUploadServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(DocumentUploadServlet.class.getName());

    private static final long DOCUMENT_MAX_SIZE = 5L * 1024L * 1024L;
    private static final long PHOTO_MAX_SIZE = 2L * 1024L * 1024L;

    private static final String IDENTITY_PROOF = "identityProof";
    private static final String ADDRESS_PROOF = "addressProof";
    private static final String PHOTOGRAPH = "photograph";

    private final DocumentDAO documentDAO = new DocumentDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (!isAuthenticated(session)) {
            response.sendRedirect("login.jsp");
            return;
        }

        if ("/admin-documents".equals(request.getServletPath())) {
            showAdminDocuments(request, response, session);
            return;
        }

        if (!isUser(session)) {
            response.sendRedirect("admin-dashboard.jsp");
            return;
        }

        showDocumentUpload(request, response, session);
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (!isAuthenticated(session)) {
            response.sendRedirect("login.jsp");
            return;
        }

        if (!"/documents".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        if (!isUser(session)) {
            response.sendRedirect("admin-dashboard.jsp");
            return;
        }

        try {
            uploadDocuments(request, response, session);
        } catch (IllegalStateException e) {
            LOGGER.log(Level.WARNING,
                    "Document upload rejected by multipart limits. userId=" + session.getAttribute("userId")
                            + ", exceptionType=" + e.getClass().getName()
                            + ", message=" + e.getMessage(),
                    e);
            request.setAttribute("errorMessage", "File size exceeds the allowed limit.");
            showDocumentUpload(request, response, session);
        }
    }

    private void showDocumentUpload(HttpServletRequest request,
                                    HttpServletResponse response,
                                    HttpSession session)
            throws ServletException, IOException {

        int userId = (Integer) session.getAttribute("userId");
        request.setAttribute("document", documentDAO.findByUserId(userId));
        request.setAttribute("documentMaxSizeMb", DOCUMENT_MAX_SIZE / (1024 * 1024));
        request.setAttribute("photoMaxSizeMb", PHOTO_MAX_SIZE / (1024 * 1024));
        request.getRequestDispatcher("document-upload.jsp").forward(request, response);
    }

    private void showAdminDocuments(HttpServletRequest request,
                                    HttpServletResponse response,
                                    HttpSession session)
            throws ServletException, IOException {

        if (!isAdmin(session)) {
            response.sendRedirect("user-dashboard.jsp");
            return;
        }

        request.setAttribute("documents", documentDAO.findAllWithUsers());
        request.getRequestDispatcher("admin-documents.jsp").forward(request, response);
    }

    private void uploadDocuments(HttpServletRequest request,
                                 HttpServletResponse response,
                                 HttpSession session)
            throws ServletException, IOException {

        int userId = (Integer) session.getAttribute("userId");
        Document existingDocument = documentDAO.findByUserId(userId);
        List<String> savedFilenames = new ArrayList<>();
        List<String> oldFilenamesToDelete = new ArrayList<>();

        try {
            boolean needsIdentityProof = existingDocument == null || isBlank(existingDocument.getIdentityProof());
            boolean needsAddressProof = existingDocument == null || isBlank(existingDocument.getAddressProof());
            boolean needsPhotograph = existingDocument == null || isBlank(existingDocument.getPhotograph());

            UploadedFile identityProof = validateAndStore(request.getPart(IDENTITY_PROOF),
                    "Identity proof", IDENTITY_PROOF, DOCUMENT_MAX_SIZE, true, needsIdentityProof);
            trackSavedFile(identityProof, savedFilenames);

            UploadedFile addressProof = validateAndStore(request.getPart(ADDRESS_PROOF),
                    "Address proof", ADDRESS_PROOF, DOCUMENT_MAX_SIZE, true, needsAddressProof);
            trackSavedFile(addressProof, savedFilenames);

            UploadedFile photograph = validateAndStore(request.getPart(PHOTOGRAPH),
                    "Passport photograph", PHOTOGRAPH, PHOTO_MAX_SIZE, false, needsPhotograph);
            trackSavedFile(photograph, savedFilenames);

            if (savedFilenames.isEmpty()) {
                throw new UploadValidationException("Choose at least one document to upload.");
            }

            Document document = new Document();
            document.setUserId(userId);
            document.setIdentityProof(selectedFilename(identityProof,
                    existingDocument == null ? null : existingDocument.getIdentityProof()));
            document.setAddressProof(selectedFilename(addressProof,
                    existingDocument == null ? null : existingDocument.getAddressProof()));
            document.setPhotograph(selectedFilename(photograph,
                    existingDocument == null ? null : existingDocument.getPhotograph()));

            if (isBlank(document.getIdentityProof()) || isBlank(document.getAddressProof())
                    || isBlank(document.getPhotograph())) {
                throw new UploadValidationException("Upload all required document types before saving.");
            }

            if (!documentDAO.upsert(document)) {
                deleteFiles(savedFilenames);
                request.setAttribute("errorMessage", "Unable to update document information.");
                showDocumentUpload(request, response, session);
                return;
            }

            trackOldFile(identityProof, existingDocument == null ? null : existingDocument.getIdentityProof(), oldFilenamesToDelete);
            trackOldFile(addressProof, existingDocument == null ? null : existingDocument.getAddressProof(), oldFilenamesToDelete);
            trackOldFile(photograph, existingDocument == null ? null : existingDocument.getPhotograph(), oldFilenamesToDelete);
            deleteFiles(oldFilenamesToDelete);
            LOGGER.info(() -> "Documents uploaded successfully. userId=" + userId);
            response.sendRedirect("documents?uploaded=1");

        } catch (SQLException e) {
            deleteFiles(savedFilenames);
            LOGGER.log(Level.SEVERE,
                    "Document metadata update failed. userId=" + userId
                            + ", sqlState=" + e.getSQLState()
                            + ", errorCode=" + e.getErrorCode()
                            + ", message=" + e.getMessage(),
                    e);
            request.setAttribute("errorMessage", databaseErrorMessage(e));
            showDocumentUpload(request, response, session);
        } catch (UploadValidationException e) {
            deleteFiles(savedFilenames);
            request.setAttribute("errorMessage", e.getMessage());
            showDocumentUpload(request, response, session);
        } catch (IOException e) {
            deleteFiles(savedFilenames);
            LOGGER.log(Level.WARNING,
                    "Document upload storage failed. userId=" + userId
                            + ", exceptionType=" + e.getClass().getName()
                            + ", message=" + e.getMessage(),
                    e);
            request.setAttribute("errorMessage", "Unable to save the uploaded document.");
            showDocumentUpload(request, response, session);
        }
    }

    private UploadedFile validateAndStore(Part part,
                                          String label,
                                          String documentType,
                                          long maxSize,
                                          boolean allowPdf,
                                          boolean required)
            throws IOException, UploadValidationException {

        if (part == null || part.getSize() == 0) {
            if (required) {
                throw new UploadValidationException(label + " is required.");
            }
            return null;
        }

        if (isBlank(part.getSubmittedFileName())) {
            throw new UploadValidationException(label + " is required.");
        }

        if (part.getSize() > maxSize) {
            throw new UploadValidationException(label + " file size exceeds the allowed limit.");
        }

        String extension = extensionFrom(part.getSubmittedFileName());
        String contentType = clean(part.getContentType()).toLowerCase();
        if (!isAllowedFile(extension, contentType, allowPdf)) {
            throw new UploadValidationException("Unsupported file type for " + label + ".");
        }

        Path uploadDirectory = uploadDirectory();
        Files.createDirectories(uploadDirectory);

        String filename = documentType + "_" + UUID.randomUUID() + "." + extension;
        Path target = uploadDirectory.resolve(filename).normalize();
        if (!target.startsWith(uploadDirectory)) {
            throw new UploadValidationException("Unable to save the uploaded document.");
        }

        try (var inputStream = part.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return new UploadedFile(filename);
    }

    private void trackSavedFile(UploadedFile uploadedFile, List<String> savedFilenames) {
        if (uploadedFile != null) {
            savedFilenames.add(uploadedFile.filename());
        }
    }

    private String selectedFilename(UploadedFile uploadedFile, String existingFilename) {
        if (uploadedFile != null) {
            return uploadedFile.filename();
        }

        return existingFilename;
    }

    private void trackOldFile(UploadedFile uploadedFile, String existingFilename, List<String> oldFilenamesToDelete) {
        if (uploadedFile != null && !isBlank(existingFilename)) {
            oldFilenamesToDelete.add(existingFilename);
        }
    }

    private boolean isAllowedFile(String extension, String contentType, boolean allowPdf) {
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            return "image/jpeg".equals(contentType);
        }

        if ("png".equals(extension)) {
            return "image/png".equals(contentType);
        }

        return allowPdf && "pdf".equals(extension) && "application/pdf".equals(contentType);
    }

    private String databaseErrorMessage(SQLException e) {
        if ("42S02".equals(e.getSQLState())) {
            return "Document database table is not available. Please run database/documents.sql, then try again.";
        }

        if ("23000".equals(e.getSQLState())) {
            return "Your login could not be matched to a valid user record. Please log out and log in again.";
        }

        return "Unable to update document information. Please contact the administrator.";
    }

    private String extensionFrom(String submittedFileName) throws UploadValidationException {
        String filename = clean(submittedFileName).replace("\\", "/");
        int slash = filename.lastIndexOf('/');
        if (slash >= 0) {
            filename = filename.substring(slash + 1);
        }

        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            throw new UploadValidationException("Unsupported file type.");
        }

        return filename.substring(dot + 1).toLowerCase();
    }

    private Path uploadDirectory() {
        String configuredDirectory = clean(System.getenv("PABS_UPLOAD_DIR"));
        if (configuredDirectory.isEmpty()) {
            configuredDirectory = Paths.get(System.getProperty("user.home"), "pabs-uploads").toString();
        }

        return Paths.get(configuredDirectory).toAbsolutePath().normalize();
    }

    private void deleteFiles(List<String> filenames) {
        Path uploadDirectory = uploadDirectory();
        for (String filename : filenames) {
            if (isBlank(filename)) {
                continue;
            }

            try {
                Path file = uploadDirectory.resolve(filename).normalize();
                if (file.startsWith(uploadDirectory)) {
                    Files.deleteIfExists(file);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING,
                        "Unable to delete uploaded document file. exceptionType=" + e.getClass().getName()
                                + ", message=" + e.getMessage(),
                        e);
            }
        }
    }

    private boolean isAuthenticated(HttpSession session) {
        return session != null && session.getAttribute("userId") != null;
    }

    private boolean isUser(HttpSession session) {
        return "USER".equalsIgnoreCase((String) session.getAttribute("role"));
    }

    private boolean isAdmin(HttpSession session) {
        return "ADMIN".equalsIgnoreCase((String) session.getAttribute("role"));
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record UploadedFile(String filename) {
    }

    private static class UploadValidationException extends Exception {
        private static final long serialVersionUID = 1L;

        UploadValidationException(String message) {
            super(message);
        }
    }
}
