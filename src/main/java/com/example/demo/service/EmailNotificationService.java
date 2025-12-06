package com.example.demo.service;

import com.example.demo.entity.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.notifications.email.enabled:true}")
    private boolean emailEnabled;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Enviar correo de alerta
     */
    @Async
    public void enviarAlerta(Usuario usuario, String asunto, String mensaje, String tipoAlerta, String colorAlerta) {
        if (!emailEnabled) {
            log.info("Notificaciones por email deshabilitadas");
            return;
        }

        if (usuario == null || usuario.getCorreo() == null || usuario.getCorreo().isEmpty()) {
            log.warn("Usuario no tiene correo configurado");
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String nombreUsuario = usuario.getNombreCompleto() != null ? usuario.getNombreCompleto() : "Usuario";
            String asuntoEmail = asunto != null ? asunto : "Notificación";
            String mensajeEmail = mensaje != null ? mensaje : "";
            String tipo = tipoAlerta != null ? tipoAlerta : "NOTIFICACIÓN";
            String color = colorAlerta != null ? colorAlerta : "azul";
            String remitente = (fromEmail != null && !fromEmail.isEmpty()) ? fromEmail : "noreply@sistema.com";
            String destinatario = usuario.getCorreo();

            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject("[" + tipo + "] " + asuntoEmail);
            helper.setText(construirHtmlEmail(nombreUsuario, mensajeEmail, tipo, color), true);

            mailSender.send(mimeMessage);
            log.info("Email enviado a {} - Asunto: {}", destinatario, asuntoEmail);

        } catch (MessagingException e) {
            log.error("Error enviando email a {}: {}", usuario.getCorreo(), e.getMessage());
        }
    }

    /**
     * Construir HTML del email
     */
    private String construirHtmlEmail(String nombreUsuario, String mensaje, String tipoAlerta, String colorAlerta) {
        String colorHex = obtenerColorHex(colorAlerta);
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: %s; color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center; }
                    .header h2 { margin: 0; font-size: 20px; }
                    .content { background-color: #f9f9f9; padding: 25px; border: 1px solid #ddd; border-top: none; }
                    .badge { display: inline-block; padding: 6px 16px; border-radius: 20px; font-size: 13px; font-weight: bold; background-color: %s; color: white; margin-bottom: 15px; }
                    .message { background-color: white; padding: 20px; border-radius: 8px; border-left: 4px solid %s; margin: 15px 0; }
                    .footer { background-color: #f1f1f1; padding: 15px; border-radius: 0 0 8px 8px; border: 1px solid #ddd; border-top: none; text-align: center; font-size: 12px; color: #666; }
                    .greeting { font-size: 16px; margin-bottom: 10px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>Sistema de Seguimiento de Reportes</h2>
                    </div>
                    <div class="content">
                        <p class="greeting">Hola <strong>%s</strong>,</p>
                        <span class="badge">%s</span>
                        <div class="message">
                            %s
                        </div>
                    </div>
                    <div class="footer">
                        <p>Este es un mensaje automático del Sistema de Seguimiento de Reportes.</p>
                        <p>Por favor no responda a este correo.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(colorHex, colorHex, colorHex, nombreUsuario, tipoAlerta, mensaje.replace("\n", "<br>"));
    }

    /**
     * Obtener color hexadecimal según tipo
     */
    private String obtenerColorHex(String colorAlerta) {
        if (colorAlerta == null) return "#3B82F6";
        
        return switch (colorAlerta.toLowerCase()) {
            case "verde", "green" -> "#10B981";
            case "amarillo", "amarilla", "yellow" -> "#F59E0B";
            case "naranja", "orange" -> "#F97316";
            case "rojo", "roja", "red" -> "#EF4444";
            case "azul", "blue" -> "#3B82F6";
            default -> "#3B82F6";
        };
    }
}
