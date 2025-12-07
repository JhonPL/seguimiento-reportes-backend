package com.example.demo.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

/**
 * Google Drive Service - MODO OAUTH CON REFRESH TOKEN
 * 
 * Este servicio usa un refresh token generado LOCALMENTE
 * y lo reutiliza en producción para evitar el flujo OAuth interactivo.
 */
@Service
public class GoogleDriveService {

    private static final String APPLICATION_NAME = "Seguimiento Reportes Llanogas";

    // Credenciales desde variables de entorno
    @Value("${google.drive.client.id:#{null}}")
    private String clientId;

    @Value("${google.drive.client.secret:#{null}}")
    private String clientSecret;

    @Value("${google.drive.refresh.token:#{null}}")
    private String refreshToken;

    @Value("${google.drive.folder.id:#{null}}")
    private String folderId;

    @Value("${google.drive.enabled:false}")
    private boolean enabled;

    private Drive driveService;

    @PostConstruct
    public void init() {
        if (!enabled) {
            System.out.println("ℹ️ Google Drive deshabilitado - Los usuarios usarán links manuales");
            return;
        }

        try {
            System.out.println("🔵 Inicializando Google Drive con OAuth (Refresh Token)...");
            
            if (clientId == null || clientSecret == null || refreshToken == null) {
                System.err.println("⚠️ Faltan credenciales de Google Drive. Configurar:");
                System.err.println("   - GOOGLE_DRIVE_CLIENT_ID");
                System.err.println("   - GOOGLE_DRIVE_CLIENT_SECRET");
                System.err.println("   - GOOGLE_DRIVE_REFRESH_TOKEN");
                return;
            }

            driveService = buildDriveService();
            System.out.println("✓ Google Drive inicializado correctamente");
            
            // Test de conexión
            testConnection();

        } catch (Exception e) {
            System.err.println("⚠️ Error inicializando Google Drive: " + e.getMessage());
            driveService = null;
        }
    }

    /**
     * Construye el servicio de Drive usando refresh token
     */
    private Drive buildDriveService() throws Exception {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        // Crear credencial con refresh token
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(GsonFactory.getDefaultInstance())
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(refreshToken);

        // Refrescar el access token
        credential.refreshToken();

        return new Drive.Builder(
                httpTransport,
                GsonFactory.getDefaultInstance(),
                credential
        ).setApplicationName(APPLICATION_NAME).build();
    }

    /**
     * Test de conexión (opcional)
     */
    private void testConnection() {
        try {
            driveService.files().list()
                    .setPageSize(1)
                    .setFields("files(id, name)")
                    .execute();
            System.out.println("✓ Conexión a Google Drive verificada");
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo verificar la conexión a Drive: " + e.getMessage());
        }
    }

    /**
     * Subir archivo a Google Drive
     */
    public Map<String, String> uploadFile(MultipartFile multipartFile, String reporteId, String periodo)
            throws IOException {

        if (!isDriveEnabled()) {
            throw new IOException("Google Drive no está configurado. Use links manuales.");
        }

        Map<String, String> result = new HashMap<>();

        try {
            String fileName = reporteId + "_" + periodo + "_" + multipartFile.getOriginalFilename();

            com.google.api.services.drive.model.File fileMeta = 
                    new com.google.api.services.drive.model.File();
            fileMeta.setName(fileName);

            // Si hay carpeta configurada, usar esa
            if (folderId != null && !folderId.isBlank()) {
                fileMeta.setParents(Collections.singletonList(folderId));
            }

            InputStreamContent content = new InputStreamContent(
                    multipartFile.getContentType(),
                    new ByteArrayInputStream(multipartFile.getBytes())
            );

            com.google.api.services.drive.model.File uploadedFile =
                    driveService.files()
                            .create(fileMeta, content)
                            .setFields("id,name,webViewLink,webContentLink")
                            .execute();

            result.put("fileId", uploadedFile.getId());
            result.put("fileName", uploadedFile.getName());
            result.put("webViewLink", uploadedFile.getWebViewLink());
            result.put("webContentLink", uploadedFile.getWebContentLink());
            result.put("mode", "oauth-refresh-token");

            System.out.println("✓ Archivo subido a Drive: " + fileName + " (ID: " + uploadedFile.getId() + ")");
            return result;

        } catch (Exception e) {
            System.err.println("✗ Error al subir archivo a Drive: " + e.getMessage());
            throw new IOException("Error al subir archivo a Google Drive: " + e.getMessage(), e);
        }
    }

    /**
     * Eliminar archivo de Google Drive
     */
    public void deleteFile(String fileId) {
        if (!isDriveEnabled()) {
            System.out.println("⚠️ Google Drive no habilitado, no se puede eliminar archivo");
            return;
        }

        try {
            driveService.files().delete(fileId).execute();
            System.out.println("✓ Archivo eliminado de Drive: " + fileId);
        } catch (Exception e) {
            System.err.println("⚠️ Error eliminando archivo de Drive: " + e.getMessage());
        }
    }

    /**
     * Verifica si Drive está habilitado y configurado
     */
    public boolean isDriveEnabled() {
        return enabled && driveService != null;
    }
}
