package com.example.ui.view;

import com.example.ui.dto.FileMetadataDto;
import com.example.ui.service.OrchestratorClient;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Route(value = "", layout = MainLayout.class)
@PageTitle("Documents | Document Server")
public class DocumentsView extends VerticalLayout {

    private final OrchestratorClient orchestratorClient;
    private final Grid<FileMetadataDto> grid;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public DocumentsView(OrchestratorClient orchestratorClient) {
        this.orchestratorClient = orchestratorClient;

        setSpacing(true);
        setPadding(true);

        // Title
        add(new H2("📤 Upload & Manage Documents"));

        // Upload section
        add(createUploadSection());

        // Grid section with refresh button
        Button refreshButton = new Button("🔄 Refresh", e -> refreshGrid());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        H2 gridTitle = new H2("📋 Documents List");
        HorizontalLayout headerLayout = new HorizontalLayout(gridTitle, refreshButton);
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(Alignment.CENTER);
        add(headerLayout);

        grid = new Grid<>(FileMetadataDto.class, false);
        configureGrid();
        add(grid);

        refreshGrid();
    }

    private VerticalLayout createUploadSection() {
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".pdf", ".docx", ".txt", ".doc", ".xlsx", ".pptx");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(100 * 1024 * 1024); // 100 MB

        upload.addSucceededListener(event -> {
            try {
                String fileName = event.getFileName();
                String mimeType = event.getMIMEType();
                long contentLength = event.getContentLength();

                // Create MultipartFile wrapper
                MultipartFile multipartFile = new MultipartFile() {
                    @Override
                    public String getName() {
                        return "file";
                    }

                    @Override
                    public String getOriginalFilename() {
                        return fileName;
                    }

                    @Override
                    public String getContentType() {
                        return mimeType;
                    }

                    @Override
                    public boolean isEmpty() {
                        return contentLength == 0;
                    }

                    @Override
                    public long getSize() {
                        return contentLength;
                    }

                    @Override
                    public byte[] getBytes() {
                        try {
                            return buffer.getInputStream().readAllBytes();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public java.io.InputStream getInputStream() {
                        return buffer.getInputStream();
                    }

                    @Override
                    public void transferTo(java.io.File dest) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public org.springframework.core.io.Resource getResource() {
                        return new org.springframework.core.io.InputStreamResource(buffer.getInputStream());
                    }
                };

                var response = orchestratorClient.uploadDocument(multipartFile);
                
                Notification notification = Notification.show(
                        "✅ File uploaded: " + fileName + " (ID: " + response.get("fileId") + ")",
                        5000,
                        Notification.Position.TOP_CENTER
                );
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                // Clear upload component to allow new uploads
                upload.clearFileList();
                
                // Refresh grid
                refreshGrid();

            } catch (Exception e) {
                log.error("Error uploading file", e);
                Notification notification = Notification.show(
                        "❌ Error: " + e.getMessage(),
                        5000,
                        Notification.Position.TOP_CENTER
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                
                // Clear upload component even on error
                upload.clearFileList();
            }
        });

        VerticalLayout uploadLayout = new VerticalLayout(upload);
        uploadLayout.setPadding(true);
        uploadLayout.setSpacing(true);
        return uploadLayout;
    }

    private void configureGrid() {
        grid.addColumn(FileMetadataDto::getOriginalFilename)
                .setHeader("Filename")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(file -> formatFileSize(file.getFileSize()))
                .setHeader("Size")
                .setAutoWidth(true);

        grid.addColumn(file -> formatStatus(file))
                .setHeader("Status")
                .setAutoWidth(true);

        grid.addColumn(file -> file.getUploadedAt() != null 
                ? file.getUploadedAt().format(DATE_FORMATTER) 
                : "")
                .setHeader("Uploaded")
                .setAutoWidth(true);

        grid.addComponentColumn(file -> {
            HorizontalLayout actions = new HorizontalLayout();

            // Download original
            Button downloadBtn = new Button("⬇️ File");
            downloadBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            downloadBtn.addClickListener(e -> downloadFile(file));
            
            // Download transcription
            Button downloadTextBtn = new Button("📄 Text");
            downloadTextBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_CONTRAST);
            downloadTextBtn.addClickListener(e -> downloadExtractedText(file));
            downloadTextBtn.setEnabled("INDEXED".equals(file.getUploadStatus()) || 
                                       "EXTRACTED".equals(file.getUploadStatus()));

            // Delete
            Button deleteBtn = new Button("🗑️");
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteBtn.addClickListener(e -> deleteFile(file));

            actions.add(downloadBtn, downloadTextBtn, deleteBtn);
            return actions;
        }).setHeader("Actions").setAutoWidth(true);

        grid.setWidthFull();
        grid.setHeight("400px");
    }

    private String formatFileSize(Long size) {
        if (size == null) return "N/A";
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }

    private String formatStatus(FileMetadataDto file) {
        String upload = file.getUploadStatus() != null ? file.getUploadStatus() : "UNKNOWN";
        String extraction = file.getExtractionStatus() != null ? file.getExtractionStatus() : "PENDING";
        String indexing = file.getIndexingStatus() != null ? file.getIndexingStatus() : "PENDING";
        
        if ("INDEXED".equals(upload)) {
            return "✅ INDEXED";
        } else if ("FAILED".equals(upload)) {
            return "❌ FAILED";
        } else if ("EXTRACTED".equals(upload)) {
            return "🔄 INDEXING";
        } else if ("UPLOADED".equals(upload)) {
            return "🔄 EXTRACTING";
        }
        return "⏳ " + upload;
    }

    private void refreshGrid() {
        try {
            List<FileMetadataDto> documents = orchestratorClient.listDocuments();
            grid.setItems(documents);
        } catch (Exception e) {
            log.error("Error loading documents", e);
            Notification.show("Error loading documents: " + e.getMessage(), 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void downloadFile(FileMetadataDto file) {
        try {
            log.info("Initiating download for file: {} (ID: {})", file.getOriginalFilename(), file.getId());
            
            // Use direct REST endpoint for download with hidden link
            String downloadUrl = "/download/" + file.getId();
            
            getUI().ifPresent(ui -> {
                ui.getPage().executeJs(
                    "const link = document.createElement('a');" +
                    "link.href = $0;" +
                    "link.download = '';" +
                    "document.body.appendChild(link);" +
                    "link.click();" +
                    "document.body.removeChild(link);",
                    downloadUrl
                );
            });
            
            Notification.show("✅ Downloading: " + file.getOriginalFilename(), 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            log.error("Error downloading file: {} (ID: {})", file.getOriginalFilename(), file.getId(), e);
            Notification.show("❌ Error downloading file: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void downloadExtractedText(FileMetadataDto file) {
        try {
            log.info("Initiating text download for file: {} (ID: {})", file.getOriginalFilename(), file.getId());
            
            // Use direct REST endpoint for download with hidden link
            String downloadUrl = "/download/" + file.getId() + "/text";
            
            getUI().ifPresent(ui -> {
                ui.getPage().executeJs(
                    "const link = document.createElement('a');" +
                    "link.href = $0;" +
                    "link.download = '';" +
                    "document.body.appendChild(link);" +
                    "link.click();" +
                    "document.body.removeChild(link);",
                    downloadUrl
                );
            });
            
            Notification.show("✅ Downloading transcription: " + file.getOriginalFilename(), 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            log.error("Error downloading text: {} (ID: {})", file.getOriginalFilename(), file.getId(), e);
            Notification.show("❌ Error downloading transcription: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void deleteFile(FileMetadataDto file) {
        try {
            orchestratorClient.deleteDocument(file.getId());
            Notification.show("✅ Document deleted: " + file.getOriginalFilename(), 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshGrid();
        } catch (Exception e) {
            log.error("Error deleting file", e);
            Notification.show("❌ Error deleting file: " + e.getMessage(), 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
