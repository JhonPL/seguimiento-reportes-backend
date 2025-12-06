package com.example.demo.service;

import com.example.demo.entity.Alerta;
import com.example.demo.entity.InstanciaReporte;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;

@Service
public class WhatsAppService {

    @Value("${twilio.account.sid:}")
    private String accountSid;

    @Value("${twilio.auth.token:}")
    private String authToken;

    @Value("${twilio.whatsapp.number:}")
    private String twilioWhatsAppNumber;

    @Value("${notificaciones.whatsapp.habilitado:false}")
    private boolean whatsappHabilitado;

    private boolean inicializado = false;

    @PostConstruct
    public void init() {
        if (estaConfigurado()) {
            try {
                com.twilio.Twilio.init(accountSid, authToken);
                inicializado = true;
                System.out.println("✓ Twilio WhatsApp inicializado correctamente");
            } catch (Exception e) {
                System.err.println("⚠️ No se pudo inicializar Twilio: " + e.getMessage());
                inicializado = false;
            }
        } else {
            System.out.println("ℹ️ WhatsApp no configurado - notificaciones por WhatsApp deshabilitadas");
        }
    }

    /**
     * Verifica si las credenciales de Twilio están configuradas
     */
    private boolean estaConfigurado() {
        return accountSid != null && !accountSid.isEmpty() && !accountSid.startsWith("AC") == false &&
               authToken != null && !authToken.isEmpty() && !authToken.equals("your_auth_token_here") &&
               twilioWhatsAppNumber != null && !twilioWhatsAppNumber.isEmpty();
    }

    /**
     * Envía notificación de alerta por WhatsApp
     */
    public void enviarNotificacionAlerta(Alerta alerta) {
        if (!estaDisponible()) {
            System.out.println("⚠️ WhatsApp no disponible - omitiendo envío");
            return;
        }

        String telefono = alerta.getUsuarioDestino().getTelefono();
        
        if (telefono == null || telefono.isEmpty()) {
            System.out.println("⚠️ Usuario no tiene teléfono configurado: " + 
                             alerta.getUsuarioDestino().getNombreCompleto());
            return;
        }

        try {
            String mensaje = generarMensajeWhatsApp(alerta);
            enviarMensaje(telefono, mensaje);
            
            System.out.println("✓ WhatsApp enviado a: " + telefono);
        } catch (Exception e) {
            System.err.println("✗ Error al enviar WhatsApp: " + e.getMessage());
        }
    }

    /**
     * Envía notificación de cambio de estado por WhatsApp
     */
    public void enviarCambioEstado(InstanciaReporte instancia, String estadoAnterior, String telefono) {
        if (!estaDisponible() || telefono == null || telefono.isEmpty()) {
            return;
        }

        try {
            String mensaje = String.format(
                "🔔 *Cambio de Estado - Llanogas*\n\n" +
                "📋 Reporte: %s\n" +
                "🏢 Entidad: %s\n" +
                "📅 Período: %s\n" +
                "⏰ Fecha Límite: %s\n\n" +
                "Estado: %s → %s\n\n" +
                "Accede al sistema para más detalles.",
                instancia.getReporte().getNombre(),
                instancia.getReporte().getEntidad().getRazonSocial(),
                instancia.getPeriodoReportado(),
                instancia.getFechaVencimientoCalculada(),
                estadoAnterior,
                instancia.getEstado().getNombre()
            );

            enviarMensaje(telefono, mensaje);
            System.out.println("✓ WhatsApp cambio estado enviado a: " + telefono);
        } catch (Exception e) {
            System.err.println("✗ Error al enviar WhatsApp: " + e.getMessage());
        }
    }

    /**
     * Envía un mensaje de WhatsApp genérico
     */
    public void enviarMensaje(String telefonoDestino, String mensaje) {
        if (!estaDisponible()) {
            System.out.println("⚠️ WhatsApp no disponible - mensaje no enviado");
            return;
        }

        try {
            // Asegurar formato correcto del teléfono
            String telefonoFormateado = formatearTelefono(telefonoDestino);
            
            com.twilio.rest.api.v2010.account.Message message = 
                com.twilio.rest.api.v2010.account.Message.creator(
                    new com.twilio.type.PhoneNumber("whatsapp:" + telefonoFormateado),
                    new com.twilio.type.PhoneNumber(twilioWhatsAppNumber),
                    mensaje
                ).create();

            System.out.println("✓ Mensaje WhatsApp enviado. SID: " + message.getSid());
        } catch (Exception e) {
            System.err.println("✗ Error enviando WhatsApp: " + e.getMessage());
            throw new RuntimeException("Error al enviar WhatsApp", e);
        }
    }

    /**
     * Genera el mensaje de WhatsApp para una alerta
     */
    private String generarMensajeWhatsApp(Alerta alerta) {
        InstanciaReporte instancia = alerta.getInstancia();
        String emoji = obtenerEmojiPorTipo(alerta.getTipo().getNombre());
        
        return String.format(
            "%s *%s - Llanogas*\n\n" +
            "Hola %s,\n\n" +
            "%s\n\n" +
            "📋 Reporte: %s\n" +
            "🏢 Entidad: %s\n" +
            "📅 Período: %s\n" +
            "⏰ Vence: %s\n" +
            "📊 Estado: %s\n\n" +
            "_Mensaje automático del Sistema de Seguimiento de Reportes_",
            emoji,
            alerta.getTipo().getNombre(),
            alerta.getUsuarioDestino().getNombreCompleto(),
            obtenerMensajeResumido(alerta),
            instancia.getReporte().getNombre(),
            instancia.getReporte().getEntidad().getRazonSocial(),
            instancia.getPeriodoReportado(),
            instancia.getFechaVencimientoCalculada(),
            instancia.getEstado().getNombre()
        );
    }

    private String obtenerMensajeResumido(Alerta alerta) {
        String tipoNombre = alerta.getTipo().getNombre().toUpperCase();
        LocalDate fechaVencimiento = alerta.getInstancia().getFechaVencimientoCalculada();
        long diasHasta = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), fechaVencimiento);

        if (tipoNombre.contains("VENCIDO") || tipoNombre.contains("CRÍTICA")) {
            return "⚠️ *URGENTE:* Este reporte está VENCIDO. Envíe inmediatamente.";
        } else if (diasHasta <= 1) {
            return "🔶 *ATENCIÓN:* Vence MAÑANA. Complete hoy.";
        } else if (diasHasta <= 5) {
            return "🟡 Recordatorio: Vence en " + diasHasta + " días.";
        } else {
            return "🟢 Inicie la recolección de información.";
        }
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

    /**
     * Formatea el número de teléfono al formato internacional requerido por Twilio
     * Ejemplo: 3001234567 → +573001234567 (Colombia)
     */
    private String formatearTelefono(String telefono) {
        // Remover espacios y caracteres especiales
        telefono = telefono.replaceAll("[^0-9+]", "");
        
        // Si ya tiene +, devolverlo
        if (telefono.startsWith("+")) {
            return telefono;
        }
        
        // Si empieza con 57 (código Colombia), agregar +
        if (telefono.startsWith("57")) {
            return "+" + telefono;
        }
        
        // Si es número local (10 dígitos), agregar código de Colombia
        if (telefono.length() == 10) {
            return "+57" + telefono;
        }
        
        // Si tiene 12 dígitos sin +, agregar +
        if (telefono.length() == 12) {
            return "+" + telefono;
        }
        
        return telefono;
    }

    /**
     * Verifica si el servicio de WhatsApp está disponible
     */
    public boolean estaDisponible() {
        return whatsappHabilitado && inicializado && estaConfigurado();
    }
}
