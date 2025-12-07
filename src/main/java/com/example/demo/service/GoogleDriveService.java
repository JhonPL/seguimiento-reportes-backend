package com.example.demo.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;

import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class GoogleDriveService {

    private static final String APPLICATION_NAME = "Seguimiento Reportes";

    @Value("${google.drive.credentials.path:#{null}}")
    private String credentialsPath;

    @Value("${google.drive.folder.id:#{null}}")
    private String folderId;

    @Value("${google.drive.mode:service}") // oauth | service
    private String mode;

    // Variable de entorno para producción
    @Value("${GOOGLE_CREDENTIALS_JSON:#{null}}")
    private String credentialsJson;

    private Drive driveService;

    // Carpeta donde se guarda el token OAuth generado
    private static final Path TOKENS_FOLDER = Path.of("tokens");

    @PostConstruct
    public void init() {
        try {
            if ("oauth".equalsIgnoreCase(mode)) {
                System.out.println("Google Drive MODO OAUTH (personal)");
                driveService = initOAuth();
            } else {
                System.out.println("Google Drive MODO SERVICE ACCOUNT (empresa)");
                driveService = initServiceAccount();
            }

            System.out.println("✓ Google Drive inicializado correctamente");

        } catch (Exception e) {
            System.err.println("Error inicializando Google Drive: " + e.getMessage());
        }
    }

    // ============================================================
    // MODO A: OAuth (usuario personal)
    // ============================================================
    private Drive initOAuth() throws Exception {

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        InputStream is = loadResource(credentialsPath);
        if (is == null) throw new FileNotFoundException("No se encontró client_secret.json");

        GoogleClientSecrets secrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(),
                new InputStreamReader(is)
        );

        // Crear carpeta de tokens
        if (!Files.exists(TOKENS_FOLDER)) Files.createDirectories(TOKENS_FOLDER);

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow
                .Builder(
                        httpTransport,
                        GsonFactory.getDefaultInstance(),
                        secrets,
                        Collections.singleton(DriveScopes.DRIVE_FILE)
                )
                .setAccessType("offline")
                .setDataStoreFactory(
                        new com.google.api.client.util.store.FileDataStoreFactory(TOKENS_FOLDER.toFile())
                )
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

        return new Drive.Builder(
                httpTransport,
                GsonFactory.getDefaultInstance(),
                new AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
        ).setApplicationName(APPLICATION_NAME).build();
    }


    // ============================================================
    // MODO B: Service Account (EMPRESA)
    // ============================================================
    private Drive initServiceAccount() throws Exception {

        InputStream is = loadResource(credentialsPath);
        if (is == null) throw new FileNotFoundException("No se encontró service-account.json");

        GoogleCredentials creds = GoogleCredentials.fromStream(is)
                .createScoped(List.of(DriveScopes.DRIVE)); // FULL acceso requerido

        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(creds)
        ).setApplicationName(APPLICATION_NAME).build();
    }


    // ============================================================
    // Utilidad para leer archivo desde resources O desde el sistema
    // ============================================================
    private InputStream loadResource(String path) throws IOException {

        if (path == null) return null;

        // Ruta absoluta en disco
        File file = new File(path);
        if (file.exists()) {
            return new FileInputStream(file);
        }

        // Resources → OBLIGATORIAMENTE debe llevar "/" al inicio
        InputStream res = getClass().getResourceAsStream("/" + path);
        return res;
    }


    // ============================================================
    // SUBIR ARCHIVO
    // ============================================================
    public Map<String, String> uploadFile(MultipartFile multipartFile, String reporteId, String periodo)
            throws IOException {

        Map<String, String> result = new HashMap<>();

        try {
            String fileName = reporteId + "_" + periodo + "_" + multipartFile.getOriginalFilename();

            com.google.api.services.drive.model.File meta = new com.google.api.services.drive.model.File();
            meta.setName(fileName);

            if (folderId != null && !folderId.isBlank()) {
                meta.setParents(Collections.singletonList(folderId));
            }

            InputStreamContent content = new InputStreamContent(
                    multipartFile.getContentType(),
                    new ByteArrayInputStream(multipartFile.getBytes())
            );

            com.google.api.services.drive.model.File uploaded =
                    driveService.files()
                            .create(meta, content)
                            .setFields("id,name,webViewLink,webContentLink")
                            .execute();

            result.put("fileId", uploaded.getId());
            result.put("fileName", uploaded.getName());
            result.put("webViewLink", uploaded.getWebViewLink());
            result.put("webContentLink", uploaded.getWebContentLink());
            result.put("mode", mode);

            return result;

        } catch (Exception e) {
            throw new IOException("Error al subir archivo: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // ELIMINAR ARCHIVO
    // ============================================================
    public void deleteFile(String fileId) {
        try {
            driveService.files().delete(fileId).execute();
        } catch (Exception e) {
            System.err.println("Error eliminando archivo: " + e.getMessage());
        }
    }

    // ============================================================
    // ¿Está Drive habilitado?
    // ============================================================
    public boolean isDriveEnabled() {
        return driveService != null;
    }
}
