package com.example.demo.service;

import com.example.demo.entity.Alerta;
import com.example.demo.entity.InstanciaReporte;
import com.example.demo.entity.NotificacionReporte;
import com.example.demo.repository.NotificacionReporteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificacionService {

    private final JavaMailSender mailSender;
    private final NotificacionReporteRepository notificacionRepo;
    private final WhatsAppService whatsAppService;

    @Value("${notificaciones.email.habilitado:false}")
    private boolean emailHabilitado;

    @Value("${spring.mail.username:}")
    private String emailRemitente;

    @Value("${notificaciones.url.base:http://localhost:5173}")
    private String urlBase;

    public NotificacionService(JavaMailSender mailSender,
                              NotificacionReporteRepository notificacionRepo,
                              WhatsAppService whatsAppService) {
        this.mailSender = mailSender;
        this.notificacionRepo = notificacionRepo;
        this.whatsAppService = whatsAppService;
    }

    /**
     * Envía notificación DUAL (Email + WhatsApp) cuando se genera una alerta
     */
    public void enviarNotificacionAlerta(Alerta alerta) {
        // 1. Enviar por Email
        if (emailHabilitado) {
            enviarEmail(alerta);
        } else {
            System.out.println("ℹ️ Email deshabilitado - omitiendo envío de email");
        }
        
        // 2. Enviar por WhatsApp
        if (whatsAppService.estaDisponible()) {
            try {
                whatsAppService.enviarNotificacionAlerta(alerta);
            } catch (Exception e) {
                System.err.println("⚠️ Error al enviar WhatsApp (continuando con email): " + e.getMessage());
            }
        }
        
        // 3. Enviar también a correos adicionales configurados
        if (emailHabilitado) {
            enviarCorreosAdicionales(alerta);
        }

        // 4. Log de la notificación (siempre se registra)
        System.out.println("📨 Notificación procesada para: " + alerta.getUsuarioDestino().getNombreCompleto());
    }

    /**
     * Envía notificación DUAL cuando cambia el estado de una instancia
     */
    public void enviarNotificacionCambioEstado(InstanciaReporte instancia, String estadoAnterior) {
        String asunto = String.format(
            "Cambio de Estado: %s - %s",
            instancia.getReporte().getNombre(),
            instancia.getPeriodoReportado()
        );

        String cuerpo = String.format(
            "Hola,\n\n" +
            "Se ha actualizado el estado del reporte:\n\n" +
            "📋 Reporte: %s\n" +
            "🏢 Entidad: %s\n" +
            "📅 Período: %s\n" +
            "⏰ Fecha Límite: %s\n\n" +
            "Estado anterior: %s\n" +
            "Estado actual: %s\n\n" +
            "Accede al sistema para más detalles: %s/reportes/%s\n\n" +
            "---\n" +
            "Sistema de Seguimiento de Reportes - Llanogas",
            instancia.getReporte().getNombre(),
            instancia.getReporte().getEntidad().getRazonSocial(),
            instancia.getPeriodoReportado(),
            instancia.getFechaVencimientoCalculada(),
            estadoAnterior,
            instancia.getEstado().getNombre(),
            urlBase,
            instancia.getId()
        );

        // Notificar al responsable de elaboración
        String emailResponsable = instancia.getReporte().getResponsableElaboracion().getCorreo();
        String telefonoResponsable = instancia.getReporte().getResponsableElaboracion().getTelefono();
        
        if (emailHabilitado) {
            enviarCorreo(emailResponsable, asunto, cuerpo);
        }
        
        if (whatsAppService.estaDisponible() && telefonoResponsable != null) {
            try {
                whatsAppService.enviarCambioEstado(instancia, estadoAnterior, telefonoResponsable);
            } catch (Exception e) {
                System.err.println("⚠️ Error al enviar WhatsApp al responsable: " + e.getMessage());
            }
        }

        // Notificar al supervisor
        String emailSupervisor = instancia.getReporte().getResponsableSupervision().getCorreo();
        String telefonoSupervisor = instancia.getReporte().getResponsableSupervision().getTelefono();
        
        if (emailHabilitado) {
            enviarCorreo(emailSupervisor, asunto, cuerpo);
        }
        
        if (whatsAppService.estaDisponible() && telefonoSupervisor != null) {
            try {
                whatsAppService.enviarCambioEstado(instancia, estadoAnterior, telefonoSupervisor);
            } catch (Exception e) {
                System.err.println("⚠️ Error al enviar WhatsApp al supervisor: " + e.getMessage());
            }
        }

        System.out.println("📨 Notificación de cambio de estado procesada");
    }

    /**
     * Envía email de alerta
     */
    private void enviarEmail(Alerta alerta) {
        if (!emailHabilitado || emailRemitente == null || emailRemitente.isEmpty()) {
            System.out.println("⚠️ Email no configurado - omitiendo envío");
            return;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(alerta.getUsuarioDestino().getCorreo());
            mensaje.setSubject(generarAsuntoAlerta(alerta));
            mensaje.setText(generarCuerpoAlerta(alerta));
            mensaje.setFrom(emailRemitente);

            mailSender.send(mensaje);
            System.out.println("✓ Email enviado a: " + alerta.getUsuarioDestino().getCorreo());

        } catch (Exception e) {
            System.err.println("✗ Error al enviar email: " + e.getMessage());
        }
    }

    private void enviarCorreosAdicionales(Alerta alerta) {
        if (!emailHabilitado) return;

        List<NotificacionReporte> notificaciones = notificacionRepo
                .findByReporte(alerta.getInstancia().getReporte());

        for (NotificacionReporte notif : notificaciones) {
            try {
                SimpleMailMessage mensaje = new SimpleMailMessage();
                mensaje.setTo(notif.getCorreo());
                mensaje.setSubject(generarAsuntoAlerta(alerta));
                mensaje.setText(generarCuerpoAlerta(alerta));
                if (emailRemitente != null && !emailRemitente.isEmpty()) {
                    mensaje.setFrom(emailRemitente);
                }

                mailSender.send(mensaje);
                System.out.println("✓ Correo adicional enviado a: " + notif.getCorreo());
            } catch (Exception e) {
                System.err.println("✗ Error al enviar correo adicional a " + notif.getCorreo());
            }
        }
    }

    private void enviarCorreo(String destinatario, String asunto, String cuerpo) {
        if (!emailHabilitado || emailRemitente == null || emailRemitente.isEmpty()) {
            System.out.println("⚠️ Email no configurado - omitiendo envío a " + destinatario);
            return;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            mensaje.setFrom(emailRemitente);

            mailSender.send(mensaje);
            System.out.println("✓ Correo enviado a: " + destinatario);
        } catch (Exception e) {
            System.err.println("✗ Error al enviar correo a " + destinatario + ": " + e.getMessage());
        }
    }

    private String generarAsuntoAlerta(Alerta alerta) {
        String emoji = obtenerEmojiPorTipo(alerta.getTipo().getNombre());
        return String.format(
            "%s %s - %s",
            emoji,
            alerta.getTipo().getNombre(),
            alerta.getInstancia().getReporte().getNombre()
        );
    }

    private String generarCuerpoAlerta(Alerta alerta) {
        InstanciaReporte instancia = alerta.getInstancia();
        
        return String.format(
            "Hola %s,\n\n" +
            "%s\n\n" +
            "Detalles del Reporte:\n" +
            "📋 Nombre: %s\n" +
            "🏢 Entidad: %s\n" +
            "📅 Período: %s\n" +
            "⏰ Fecha Límite: %s\n" +
            "📊 Estado Actual: %s\n" +
            "⚖️ Base Legal: %s\n\n" +
            "Accede al sistema para gestionar este reporte: %s/reportes/%s\n\n" +
            "---\n" +
            "Sistema de Seguimiento de Reportes - Llanogas\n" +
            "Este es un mensaje automático, por favor no responder.",
            alerta.getUsuarioDestino().getNombreCompleto(),
            alerta.getMensaje(),
            instancia.getReporte().getNombre(),
            instancia.getReporte().getEntidad().getRazonSocial(),
            instancia.getPeriodoReportado(),
            instancia.getFechaVencimientoCalculada(),
            instancia.getEstado().getNombre(),
            instancia.getReporte().getBaseLegal(),
            urlBase,
            instancia.getId()
        );
    }

    private String obtenerEmojiPorTipo(String tipoNombre) {
        if (tipoNombre.contains("Crítica") || tipoNombre.contains("Vencido")) {
            return "🔴";
        } else if (tipoNombre.contains("Urgente") || tipoNombre.contains("Riesgo")) {
            return "🟠";
        } else if (tipoNombre.contains("Seguimiento") || tipoNombre.contains("Intermedia")) {
            return "🟡";
        } else {
            return "🟢";
        }
    }
}
