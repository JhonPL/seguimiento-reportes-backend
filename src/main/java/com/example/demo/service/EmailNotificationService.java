package com.example.demo.service;

import com.example.demo.entity.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio de Email usando Resend HTTP API
 * 
 * VENTAJA: No usa SMTP (puerto bloqueado en Render Free)
 * USA: API REST de Resend sobre HTTPS (puerto 443 - siempre abierto)
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.from.email:onboarding@resend.dev}")
    private String fromEmail;

    @Value("${resend.from.name:Sistema Llanogas}")
    private String fromName;

    @Value("${notificaciones.email.habilitado:false}")
    private boolean emailEnabled;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Enviar correo de alerta usando Resend HTTP API
     */
    @Async
    public void enviarAlerta(Usuario usuario, String asunto, String mensaje, String tipoAlerta, String colorAlerta) {
        log.info("📧 Intentando enviar email vía Resend API...");
        log.info("   Habilitado: {}", emailEnabled);
        log.info("   API Key configurada: {}", resendApiKey != null && !resendApiKey.isEmpty());
        log.info("   Destinatario: {}", usuario != null ? usuario.getCorreo() : "null");
        
        if (!emailEnabled) {
            log.warn("⚠️ Notificaciones por email DESHABILITADAS");
            log.warn("⚠️ Configure NOTIFICATIONS_EMAIL_ENABLED=true en Render");
            return;
        }

        if (resendApiKey == null || resendApiKey.isEmpty()) {
            log.error("❌ RESEND_API_KEY NO configurada");
            log.error("❌ Configure RESEND_API_KEY en Render con tu API key de resend.com");
            return;
        }

        if (usuario == null || usuario.getCorreo() == null || usuario.getCorreo().isEmpty()) {
            log.error("❌ Usuario no tiene correo configurado: {}", usuario != null ? usuario.getNombreCompleto() : "null");
            return;
        }

        try {
            String nombreUsuario = usuario.getNombreCompleto() != null ? usuario.getNombreCompleto() : "Usuario";
            String asuntoEmail = asunto != null ? asunto : "Notificación";
            String mensajeEmail = mensaje != null ? mensaje : "";
            String tipo = tipoAlerta != null ? tipoAlerta : "NOTIFICACIÓN";
            String color = colorAlerta != null ? colorAlerta : "azul";
            String destinatario = usuario.getCorreo();

            // Construir el body para Resend API
            Map<String, Object> emailData = new HashMap<>();
            emailData.put("from", fromName + " <" + fromEmail + ">");
            emailData.put("to", new String[]{destinatario});
            emailData.put("subject", "[" + tipo + "] " + asuntoEmail);
            emailData.put("html", construirHtmlEmail(nombreUsuario, mensajeEmail, tipo, color));

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailData, headers);

            log.info("📤 Enviando email vía Resend API a {}", destinatario);
            
            // Enviar a Resend API
            ResponseEntity<String> response = restTemplate.postForEntity(
                "https://api.resend.com/emails",
                request,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                log.info("✅ Email enviado exitosamente a {} - Response: {}", destinatario, response.getBody());
            } else {
                log.error("❌ Error en respuesta de Resend: {} - {}", response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("❌ Error enviando email vía Resend API: {}", e.getMessage());
            log.error("❌ Detalles: ", e);
        }
    }

    /**
     * Construir HTML del email
     */
    private String construirHtmlEmail(String nombreUsuario, String mensaje, String tipoAlerta, String colorAlerta) {
        String colorHex = obtenerColorHex(colorAlerta);
        
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<style>" +
                    "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }" +
                    ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                    ".header { background-color: " + colorHex + "; color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center; }" +
                    ".header h2 { margin: 0; font-size: 20px; }" +
                    ".content { background-color: #f9f9f9; padding: 25px; border: 1px solid #ddd; border-top: none; }" +
                    ".badge { display: inline-block; padding: 6px 16px; border-radius: 20px; font-size: 13px; font-weight: bold; background-color: " + colorHex + "; color: white; margin-bottom: 15px; }" +
                    ".message { background-color: white; padding: 20px; border-radius: 8px; border-left: 4px solid " + colorHex + "; margin: 15px 0; }" +
                    ".footer { background-color: #f1f1f1; padding: 15px; border-radius: 0 0 8px 8px; border: 1px solid #ddd; border-top: none; text-align: center; font-size: 12px; color: #666; }" +
                    ".greeting { font-size: 16px; margin-bottom: 10px; }" +
                "</style>" +
            "</head>" +
            "<body>" +
                "<div class=\"container\">" +
                    "<div class=\"header\">" +
                        "<h2>Sistema de Seguimiento de Reportes - Llanogas</h2>" +
                    "</div>" +
                    "<div class=\"content\">" +
                        "<p class=\"greeting\">Hola <strong>" + nombreUsuario + "</strong>,</p>" +
                        "<span class=\"badge\">" + tipoAlerta + "</span>" +
                        "<div class=\"message\">" +
                            mensaje.replace("\n", "<br>") +
                        "</div>" +
                    "</div>" +
                    "<div class=\"footer\">" +
                        "<p>Este es un mensaje automático del Sistema de Seguimiento de Reportes.</p>" +
                        "<p>Por favor no responda a este correo.</p>" +
                    "</div>" +
                "</div>" +
            "</body>" +
            "</html>";
    }

    /**
     * Obtener color hexadecimal según tipo
     */
    private String obtenerColorHex(String colorAlerta) {
        if (colorAlerta == null) return "#3B82F6";
        
        switch (colorAlerta.toLowerCase()) {
            case "verde":
            case "green":
                return "#10B981";
            case "amarillo":
            case "amarilla":
            case "yellow":
                return "#F59E0B";
            case "naranja":
            case "orange":
                return "#F97316";
            case "rojo":
            case "roja":
            case "red":
                return "#EF4444";
            case "azul":
            case "blue":
                return "#3B82F6";
            default:
                return "#3B82F6";
        }
    }
}
