package com.example.demo.service;

import com.example.demo.entity.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;

    //@Value("${spring.mail.username:}")
    //private String fromEmail;

    @Value("${spring.mail.from:onboarding@resend.dev}")
    private String fromEmail;

    // ⚠️ CAMBIO CRÍTICO #1: Usar la variable correcta
    @Value("${notificaciones.email.habilitado:false}")
    private boolean emailEnabled;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ✅ CAMBIO CRÍTICO #2: Agregar logs de inicialización
    @PostConstruct
    public void init() {
        log.info("====================================================");
        log.info("EmailNotificationService inicializado");
        log.info("Email habilitado: {}", emailEnabled);
        log.info("From email: {}", fromEmail != null && !fromEmail.isEmpty() ? fromEmail : "NO CONFIGURADO");
        log.info("====================================================");
        
        if (!emailEnabled) {
            log.warn("⚠️ NOTIFICACIONES POR EMAIL DESHABILITADAS");
            log.warn("⚠️ Configure NOTIFICATIONS_EMAIL_ENABLED=true en Render");
        }
    }

    // ✅ CAMBIO CRÍTICO #3: Agregar logs en enviarAlerta
    @Async
    public void enviarAlerta(Usuario usuario, String asunto, String mensaje, String tipoAlerta, String colorAlerta) {
        // 🔍 LOGS PARA DEBUG
        log.debug("🔍 Intentando enviar email a {}", usuario != null ? usuario.getCorreo() : "NULL");
        
        if (!emailEnabled) {
            log.info("ℹ️ Notificaciones por email deshabilitadas - omitiendo envío");
            return;
        }

        if (usuario == null || usuario.getCorreo() == null || usuario.getCorreo().isEmpty()) {
            log.warn("⚠️ Usuario no tiene correo configurado");
            return;
        }
        
        // 🔍 VALIDACIÓN ADICIONAL
        if (fromEmail == null || fromEmail.isEmpty()) {
            log.error("⚠️ SMTP_USER no configurado - no se puede enviar email");
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
            String destinatario = usuario.getCorreo();

            helper.setFrom(fromEmail);
            helper.setTo(destinatario);
            helper.setSubject("[" + tipo + "] " + asuntoEmail);
            helper.setText(construirHtmlEmail(nombreUsuario, mensajeEmail, tipo, color), true);

            // 🔍 LOG ANTES DE ENVIAR
            log.info("📧 Enviando email a {} - Asunto: {}", destinatario, asuntoEmail);
            
            mailSender.send(mimeMessage);
            
            // ✅ LOG DE ÉXITO
            log.info("✅ Email enviado exitosamente a {}", destinatario);

        } catch (MessagingException e) {
            log.error("❌ Error enviando email a {}: {}", usuario.getCorreo(), e.getMessage());
        } catch (Exception e) {
            log.error("❌ Error inesperado: {}", e.getMessage());
        }
    }

    // ✅ OPCIONAL PERO ÚTIL: Método para verificar si está habilitado
    public boolean estaHabilitado() {
        return emailEnabled;
    }

    // EL RESTO DEL CÓDIGO SE QUEDA IGUAL (construirHtmlEmail, obtenerColorHex, etc.)
    
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
