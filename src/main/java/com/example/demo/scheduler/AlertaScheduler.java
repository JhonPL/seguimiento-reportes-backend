package com.example.demo.scheduler;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.service.EmailNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Scheduler que genera y envía alertas por EMAIL automáticamente.
 * 
 * MEJORADO PARA PRODUCCIÓN:
 * - Ejecuta cada 2 horas para evitar problemas de sleep en Render
 * - Verifica última ejecución para evitar duplicados
 * - Logs detallados para debugging
 */
@Component
public class AlertaScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertaScheduler.class);

    private final InstanciaReporteRepository instanciaRepository;
    private final AlertaRepository alertaRepository;
    private final TipoAlertaRepository tipoAlertaRepository;
    private final EmailNotificationService emailService;

    @Value("${notificaciones.email.habilitado:false}")
    private boolean emailHabilitado;

    // Variable para trackear última ejecución
    private LocalDateTime ultimaEjecucion = null;

    public AlertaScheduler(InstanciaReporteRepository instanciaRepository,
                          AlertaRepository alertaRepository,
                          TipoAlertaRepository tipoAlertaRepository,
                          EmailNotificationService emailService) {
        this.instanciaRepository = instanciaRepository;
        this.alertaRepository = alertaRepository;
        this.tipoAlertaRepository = tipoAlertaRepository;
        this.emailService = emailService;
    }

    @PostConstruct
    public void init() {
        log.info("====================================================");
        log.info("AlertaScheduler inicializado");
        log.info("Email habilitado: {}", emailHabilitado);
        log.info("Scheduler ejecutará cada 2 horas");
        log.info("====================================================");
    }

    /**
     * MEJORADO: Ejecuta cada 2 horas en lugar de una vez al día
     * Esto garantiza que funcione incluso con el sleep de Render Free
     */
    @Scheduled(cron = "0 10 12 * * *") // Cada 2 horas
    @Transactional
    public void generarAlertasDiarias() {
        LocalDateTime ahora = LocalDateTime.now();
        
        // Evitar ejecuciones duplicadas en la misma hora
        if (ultimaEjecucion != null && 
            ChronoUnit.HOURS.between(ultimaEjecucion, ahora) < 1) {
            log.debug("Scheduler ya ejecutado hace menos de 1 hora, omitiendo");
            return;
        }
        
        log.info("=== Iniciando generación de alertas ===");
        log.info("Fecha/Hora: {}", ahora);
        log.info("Email habilitado: {}", emailHabilitado);
        
        if (!emailHabilitado) {
            log.warn("⚠️ Email deshabilitado. Las alertas se crearán pero NO se enviarán correos.");
            log.warn("⚠️ Configure NOTIFICATIONS_EMAIL_ENABLED=true en Render");
        }
        
        LocalDate hoy = LocalDate.now();
        
        // Obtener todas las instancias pendientes (sin fecha de envío real)
        List<InstanciaReporte> instanciasPendientes = instanciaRepository.findByFechaEnvioRealIsNull();
        
        log.info("Procesando {} instancias pendientes", instanciasPendientes.size());
        
        int alertasGeneradas = 0;
        
        for (InstanciaReporte instancia : instanciasPendientes) {
            try {
                alertasGeneradas += procesarInstancia(instancia, hoy);
            } catch (Exception e) {
                log.error("Error procesando instancia {}: {}", instancia.getId(), e.getMessage(), e);
            }
        }
        
        ultimaEjecucion = ahora;
        log.info("=== Generación de alertas completada. {} alertas enviadas ===", alertasGeneradas);
    }

    /**
     * Procesar una instancia y generar alertas según corresponda
     */
    private int procesarInstancia(InstanciaReporte instancia, LocalDate hoy) {
        LocalDate fechaVencimiento = instancia.getFechaVencimientoCalculada();
        if (fechaVencimiento == null) return 0;
        
        // Verificar que el reporte existe y está activo
        Reporte reporte = instancia.getReporte();
        if (reporte == null || !reporte.isActivo()) return 0;
        
        long diasHastaVencimiento = ChronoUnit.DAYS.between(hoy, fechaVencimiento);
        
        // Los responsables están en el Reporte, no en la InstanciaReporte
        Usuario responsable = reporte.getResponsableElaboracion();
        Usuario supervisor = reporte.getResponsableSupervision();
        
        int alertasGeneradas = 0;
        
        // === ALERTAS PARA RESPONSABLE ===
        if (responsable != null) {
            // Alerta Preventiva (15 o 10 días antes)
            if (diasHastaVencimiento == 15 || diasHastaVencimiento == 10) {
                if (generarAlertaResponsable(instancia, responsable, "PREVENTIVA", "verde",
                    construirMensajePreventiva(instancia, fechaVencimiento, diasHastaVencimiento))) {
                    alertasGeneradas++;
                }
            }
            
            // Alerta Seguimiento (5 días antes)
            if (diasHastaVencimiento == 5) {
                if (generarAlertaResponsable(instancia, responsable, "SEGUIMIENTO", "amarillo",
                    construirMensajeSeguimiento(instancia, diasHastaVencimiento))) {
                    alertasGeneradas++;
                }
            }
            
            // Alerta Riesgo (1 día antes)
            if (diasHastaVencimiento == 1) {
                if (generarAlertaResponsable(instancia, responsable, "RIESGO", "naranja",
                    construirMensajeRiesgo(instancia))) {
                    alertasGeneradas++;
                }
            }
            
            // Alerta Crítica (Vencido - se envía diariamente)
            if (diasHastaVencimiento < 0) {
                long diasVencido = Math.abs(diasHastaVencimiento);
                if (generarAlertaResponsable(instancia, responsable, "CRITICA", "rojo",
                    construirMensajeCritica(instancia, diasVencido))) {
                    alertasGeneradas++;
                }
            }
        }
        
        // === ALERTAS PARA SUPERVISOR ===
        if (supervisor != null) {
            // Alerta Supervisión (5 días antes)
            if (diasHastaVencimiento == 5) {
                if (generarAlertaSupervisor(instancia, supervisor,
                    construirMensajeSupervisor5Dias(instancia, responsable))) {
                    alertasGeneradas++;
                }
            }
            
            // Alerta Supervisión (1 día antes)
            if (diasHastaVencimiento == 1) {
                if (generarAlertaSupervisor(instancia, supervisor,
                    construirMensajeSupervisor1Dia(instancia, responsable))) {
                    alertasGeneradas++;
                }
            }
        }
        
        return alertasGeneradas;
    }

    /**
     * Generar alerta para responsable
     */
    private boolean generarAlertaResponsable(InstanciaReporte instancia, Usuario responsable, 
                                              String tipoNombre, String color, String mensaje) {
        // Verificar si ya existe una alerta igual hoy
        if (existeAlertaHoy(instancia, responsable, tipoNombre)) {
            log.debug("Ya existe alerta {} para instancia {} hoy", tipoNombre, instancia.getId());
            return false;
        }
        
        TipoAlerta tipoAlerta = obtenerOCrearTipoAlerta(tipoNombre, color);
        
        Alerta alerta = new Alerta();
        alerta.setInstancia(instancia);
        alerta.setTipo(tipoAlerta);
        alerta.setUsuarioDestino(responsable);
        alerta.setMensaje(mensaje);
        alerta.setFechaProgramada(LocalDateTime.now());
        alerta.setFechaEnviada(LocalDateTime.now());
        alerta.setEnviada(true);
        alerta.setLeida(false);
        
        alertaRepository.save(alerta);
        
        // Enviar email
        String asunto = construirAsunto(instancia);
        emailService.enviarAlerta(responsable, asunto, mensaje, tipoNombre, color);
        
        String nombreReporte = instancia.getReporte() != null ? instancia.getReporte().getNombre() : String.valueOf(instancia.getId());
        log.info("✓ Alerta {} enviada a {} para reporte {}", tipoNombre, responsable.getNombreCompleto(), nombreReporte);
        
        return true;
    }

    /**
     * Generar alerta para supervisor
     */
    private boolean generarAlertaSupervisor(InstanciaReporte instancia, Usuario supervisor, String mensaje) {
        String tipoNombre = "SUPERVISION";
        
        if (existeAlertaHoy(instancia, supervisor, tipoNombre)) {
            return false;
        }
        
        TipoAlerta tipoAlerta = obtenerOCrearTipoAlerta(tipoNombre, "azul");
        
        Alerta alerta = new Alerta();
        alerta.setInstancia(instancia);
        alerta.setTipo(tipoAlerta);
        alerta.setUsuarioDestino(supervisor);
        alerta.setMensaje(mensaje);
        alerta.setFechaProgramada(LocalDateTime.now());
        alerta.setFechaEnviada(LocalDateTime.now());
        alerta.setEnviada(true);
        alerta.setLeida(false);
        
        alertaRepository.save(alerta);
        
        // Enviar email
        String asunto = "Supervisión: " + construirAsunto(instancia);
        emailService.enviarAlerta(supervisor, asunto, mensaje, tipoNombre, "azul");
        
        String nombreReporte = instancia.getReporte() != null ? instancia.getReporte().getNombre() : String.valueOf(instancia.getId());
        log.info("✓ Alerta SUPERVISION enviada a {} para reporte {}", supervisor.getNombreCompleto(), nombreReporte);
        
        return true;
    }

    /**
     * Verificar si ya existe una alerta igual hoy
     */
    private boolean existeAlertaHoy(InstanciaReporte instancia, Usuario usuario, String tipoNombre) {
        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = inicioHoy.plusDays(1);
        
        return alertaRepository.existsByInstanciaAndUsuarioDestinoAndTipoNombreAndFechaEnviadaBetween(
                instancia, usuario, tipoNombre, inicioHoy, finHoy);
    }

    /**
     * Obtener o crear tipo de alerta
     */
    private TipoAlerta obtenerOCrearTipoAlerta(String nombre, String color) {
        Optional<TipoAlerta> existente = tipoAlertaRepository.findByNombre(nombre);
        
        if (existente.isPresent()) {
            return existente.get();
        }
        
        TipoAlerta nuevo = new TipoAlerta();
        nuevo.setNombre(nombre);
        nuevo.setColor(color);
        nuevo.setDiasAntesVencimiento(obtenerDiasAntes(nombre));
        nuevo.setEsPostVencimiento(nombre.equals("CRITICA"));
        
        return tipoAlertaRepository.save(nuevo);
    }

    private Integer obtenerDiasAntes(String nombre) {
        return switch (nombre) {
            case "PREVENTIVA" -> 10;
            case "SEGUIMIENTO" -> 5;
            case "RIESGO" -> 1;
            case "CRITICA" -> 0;
            case "SUPERVISION" -> 5;
            default -> 0;
        };
    }

    /**
     * Construir asunto del email
     */
    private String construirAsunto(InstanciaReporte instancia) {
        StringBuilder asunto = new StringBuilder();
        
        if (instancia.getReporte() != null) {
            asunto.append(instancia.getReporte().getNombre());
        } else {
            asunto.append("Reporte");
        }
        
        if (instancia.getPeriodoReportado() != null) {
            asunto.append(" - ").append(instancia.getPeriodoReportado());
        }
        
        return asunto.toString();
    }

    // ==================== CONSTRUCCIÓN DE MENSAJES ====================

    private String construirMensajePreventiva(InstanciaReporte instancia, LocalDate fechaVencimiento, long diasRestantes) {
        Reporte reporte = instancia.getReporte();
        String nombreReporte = reporte != null ? reporte.getNombre() : "Reporte";
        String baseLegal = reporte != null && reporte.getBaseLegal() != null ? reporte.getBaseLegal() : "";
        String entidad = reporte != null && reporte.getEntidad() != null ? reporte.getEntidad().getRazonSocial() : "";
        
        StringBuilder sb = new StringBuilder();
        sb.append("RECORDATORIO: Vencimiento de <strong>").append(nombreReporte).append("</strong> el ")
          .append(fechaVencimiento.toString()).append(" (").append(diasRestantes).append(" días restantes).\n\n");
        
        sb.append("Inicie la recolección de la información");
        if (!baseLegal.isEmpty()) {
            sb.append(", recuerde que se debe dar cumplimiento a ").append(baseLegal);
        }
        sb.append(".\n\n");
        
        if (!entidad.isEmpty()) {
            sb.append("<strong>Entidad:</strong> ").append(entidad).append("\n");
        }
        if (instancia.getPeriodoReportado() != null) {
            sb.append("<strong>Periodo:</strong> ").append(instancia.getPeriodoReportado());
        }
        
        return sb.toString();
    }

    private String construirMensajeSeguimiento(InstanciaReporte instancia, long diasRestantes) {
        Reporte reporte = instancia.getReporte();
        String nombreReporte = reporte != null ? reporte.getNombre() : "Reporte";
        String entidad = reporte != null && reporte.getEntidad() != null ? reporte.getEntidad().getRazonSocial() : "";
        
        StringBuilder sb = new StringBuilder();
        sb.append("ATENCIÓN: <strong>").append(nombreReporte).append("</strong> vence en ")
          .append(diasRestantes).append(" días.\n\n");
        sb.append("<strong>Estado actual:</strong> PENDIENTE\n");
        sb.append("No olvide avanzar en la elaboración del reporte.\n\n");
        
        if (!entidad.isEmpty()) {
            sb.append("<strong>Entidad:</strong> ").append(entidad).append("\n");
        }
        if (instancia.getPeriodoReportado() != null) {
            sb.append("<strong>Periodo:</strong> ").append(instancia.getPeriodoReportado());
        }
        
        return sb.toString();
    }

    private String construirMensajeRiesgo(InstanciaReporte instancia) {
        Reporte reporte = instancia.getReporte();
        String nombreReporte = reporte != null ? reporte.getNombre() : "Reporte";
        String entidad = reporte != null && reporte.getEntidad() != null ? reporte.getEntidad().getRazonSocial() : "";
        
        StringBuilder sb = new StringBuilder();
        sb.append("¡URGENTE! <strong>").append(nombreReporte).append("</strong> vence MAÑANA.\n\n");
        sb.append("Debe enviar el reporte antes de la fecha límite para evitar incumplimiento.\n\n");
        
        if (!entidad.isEmpty()) {
            sb.append("<strong>Entidad:</strong> ").append(entidad).append("\n");
        }
        if (instancia.getPeriodoReportado() != null) {
            sb.append("<strong>Periodo:</strong> ").append(instancia.getPeriodoReportado());
        }
        
        return sb.toString();
    }

    private String construirMensajeCritica(InstanciaReporte instancia, long diasVencido) {
        Reporte reporte = instancia.getReporte();
        String nombreReporte = reporte != null ? reporte.getNombre() : "Reporte";
        String entidad = reporte != null && reporte.getEntidad() != null ? reporte.getEntidad().getRazonSocial() : "";
        
        StringBuilder sb = new StringBuilder();
        sb.append("¡ALERTA ROJA! <strong>").append(nombreReporte).append("</strong> está VENCIDO desde hace ")
          .append(diasVencido).append(" día(s).\n\n");
        sb.append("<strong>Envíe de inmediato.</strong>\n\n");
        
        if (!entidad.isEmpty()) {
            sb.append("<strong>Entidad:</strong> ").append(entidad).append("\n");
        }
        if (instancia.getPeriodoReportado() != null) {
            sb.append("<strong>Periodo:</strong> ").append(instancia.getPeriodoReportado());
        }
        
        return sb.toString();
    }

    private String construirMensajeSupervisor5Dias(InstanciaReporte instancia, Usuario responsable) {
        Reporte reporte = instancia.getReporte();
        String nombreReporte = reporte != null ? reporte.getNombre() : "Reporte";
        String nombreResponsable = responsable != null ? responsable.getNombreCompleto() : "Sin asignar";
        String entidad = reporte != null && reporte.getEntidad() != null ? reporte.getEntidad().getRazonSocial() : "";
        
        StringBuilder sb = new StringBuilder();
        sb.append("TAREA PENDIENTE: <strong>").append(nombreReporte).append("</strong> requiere su seguimiento.\n\n");
        sb.append("<strong>Responsable de elaboración:</strong> ").append(nombreResponsable).append("\n");
        sb.append("<strong>Estado:</strong> Pendiente\n");
        sb.append("<strong>Vence en:</strong> 5 días\n\n");
        
        if (!entidad.isEmpty()) {
            sb.append("<strong>Entidad:</strong> ").append(entidad).append("\n");
        }
        if (instancia.getPeriodoReportado() != null) {
            sb.append("<strong>Periodo:</strong> ").append(instancia.getPeriodoReportado());
        }
        
        return sb.toString();
    }

    private String construirMensajeSupervisor1Dia(InstanciaReporte instancia, Usuario responsable) {
        Reporte reporte = instancia.getReporte();
        String nombreReporte = reporte != null ? reporte.getNombre() : "Reporte";
        String nombreResponsable = responsable != null ? responsable.getNombreCompleto() : "Sin asignar";
        String entidad = reporte != null && reporte.getEntidad() != null ? reporte.getEntidad().getRazonSocial() : "";
        
        StringBuilder sb = new StringBuilder();
        sb.append("ALERTA: El reporte <strong>").append(nombreReporte).append("</strong> sigue en estado PENDIENTE y vence MAÑANA.\n\n");
        sb.append("<strong>Responsable:</strong> ").append(nombreResponsable).append("\n\n");
        sb.append("Se requiere su intervención para asegurar el cumplimiento.\n\n");
        
        if (!entidad.isEmpty()) {
            sb.append("<strong>Entidad:</strong> ").append(entidad).append("\n");
        }
        if (instancia.getPeriodoReportado() != null) {
            sb.append("<strong>Periodo:</strong> ").append(instancia.getPeriodoReportado());
        }
        
        return sb.toString();
    }
}
