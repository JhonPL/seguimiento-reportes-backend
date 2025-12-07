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
 * Scheduler MEJORADO para Render Free Tier
 * - Ejecuta cada 2 horas para evitar el sleep de Render
 * - Evita duplicados con verificación de última ejecución
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
        log.info("✓ AlertaScheduler inicializado");
        log.info("✓ Email habilitado: {}", emailHabilitado);
        log.info("✓ Zona horaria: {}", java.util.TimeZone.getDefault().getID());
        log.info("✓ Scheduler ejecutará cada 2 horas");
        log.info("====================================================");
        
        // Ejecutar inmediatamente al iniciar (útil para testing)
        if (emailHabilitado) {
            log.info("🚀 Ejecutando generación de alertas al iniciar...");
            new Thread(() -> {
                try {
                    Thread.sleep(5000); // Esperar 5 segundos para que el sistema termine de iniciar
                    generarAlertasDiarias();
                } catch (Exception e) {
                    log.error("Error en ejecución inicial: {}", e.getMessage());
                }
            }).start();
        }
    }

    /**
     * MEJORADO: Ejecuta cada 2 horas
     * Cron: 0 0 */2 * * * = cada 2 horas en punto
     */
    @Scheduled(cron = "0 0 */2 * * *")
    @Transactional
    public void generarAlertasDiarias() {
        LocalDateTime ahora = LocalDateTime.now();
        
        // Evitar ejecuciones duplicadas en la misma hora
        if (ultimaEjecucion != null && 
            ChronoUnit.MINUTES.between(ultimaEjecucion, ahora) < 30) {
            log.debug("⏭️ Scheduler ya ejecutado hace menos de 30 minutos, omitiendo");
            return;
        }
        
        log.info("=================================================");
        log.info("🔔 Iniciando generación de alertas");
        log.info("📅 Fecha/Hora: {}", ahora);
        log.info("✉️ Email habilitado: {}", emailHabilitado);
        log.info("=================================================");
        
        if (!emailHabilitado) {
            log.warn("⚠️ Email deshabilitado. Configure NOTIFICATIONS_EMAIL_ENABLED=true");
            return;
        }
        
        LocalDate hoy = LocalDate.now();
        List<InstanciaReporte> instanciasPendientes = instanciaRepository.findByFechaEnvioRealIsNull();
        
        log.info("📊 Instancias pendientes encontradas: {}", instanciasPendientes.size());
        
        int alertasGeneradas = 0;
        int errores = 0;
        
        for (InstanciaReporte instancia : instanciasPendientes) {
            try {
                alertasGeneradas += procesarInstancia(instancia, hoy);
            } catch (Exception e) {
                errores++;
                log.error("❌ Error procesando instancia {}: {}", instancia.getId(), e.getMessage());
            }
        }
        
        ultimaEjecucion = ahora;
        
        log.info("=================================================");
        log.info("✅ Generación de alertas completada");
        log.info("📧 Alertas enviadas: {}", alertasGeneradas);
        if (errores > 0) {
            log.warn("⚠️ Errores encontrados: {}", errores);
        }
        log.info("=================================================");
    }

    private int procesarInstancia(InstanciaReporte instancia, LocalDate hoy) {
        LocalDate fechaVencimiento = instancia.getFechaVencimientoCalculada();
        if (fechaVencimiento == null) return 0;
        
        Reporte reporte = instancia.getReporte();
        if (reporte == null || !reporte.isActivo()) return 0;
        
        long diasHastaVencimiento = ChronoUnit.DAYS.between(hoy, fechaVencimiento);
        
        Usuario responsable = reporte.getResponsableElaboracion();
        Usuario supervisor = reporte.getResponsableSupervision();
        
        int alertasGeneradas = 0;
        
        // === ALERTAS PARA RESPONSABLE ===
        if (responsable != null) {
            // Preventiva (15 o 10 días antes)
            if (diasHastaVencimiento == 15 || diasHastaVencimiento == 10) {
                if (generarAlertaResponsable(instancia, responsable, "PREVENTIVA", "verde",
                    construirMensajePreventiva(instancia, fechaVencimiento, diasHastaVencimiento))) {
                    alertasGeneradas++;
                }
            }
            
            // Seguimiento (5 días antes)
            if (diasHastaVencimiento == 5) {
                if (generarAlertaResponsable(instancia, responsable, "SEGUIMIENTO", "amarillo",
                    construirMensajeSeguimiento(instancia, diasHastaVencimiento))) {
                    alertasGeneradas++;
                }
            }
            
            // Riesgo (1 día antes)
            if (diasHastaVencimiento == 1) {
                if (generarAlertaResponsable(instancia, responsable, "RIESGO", "naranja",
                    construirMensajeRiesgo(instancia))) {
                    alertasGeneradas++;
                }
            }
            
            // Crítica (Vencido)
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
            if (diasHastaVencimiento == 5) {
                if (generarAlertaSupervisor(instancia, supervisor,
                    construirMensajeSupervisor5Dias(instancia, responsable))) {
                    alertasGeneradas++;
                }
            }
            
            if (diasHastaVencimiento == 1) {
                if (generarAlertaSupervisor(instancia, supervisor,
                    construirMensajeSupervisor1Dia(instancia, responsable))) {
                    alertasGeneradas++;
                }
            }
        }
        
        return alertasGeneradas;
    }

    private boolean generarAlertaResponsable(InstanciaReporte instancia, Usuario responsable, 
                                              String tipoNombre, String color, String mensaje) {
        if (existeAlertaHoy(instancia, responsable, tipoNombre)) {
            log.debug("⏭️ Ya existe alerta {} para instancia {} hoy", tipoNombre, instancia.getId());
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
        
        String asunto = construirAsunto(instancia);
        emailService.enviarAlerta(responsable, asunto, mensaje, tipoNombre, color);
        
        String nombreReporte = instancia.getReporte() != null ? instancia.getReporte().getNombre() : String.valueOf(instancia.getId());
        log.info("✅ Alerta {} enviada a {} para reporte {}", tipoNombre, responsable.getNombreCompleto(), nombreReporte);
        
        return true;
    }

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
        
        String asunto = "Supervisión: " + construirAsunto(instancia);
        emailService.enviarAlerta(supervisor, asunto, mensaje, tipoNombre, "azul");
        
        String nombreReporte = instancia.getReporte() != null ? instancia.getReporte().getNombre() : String.valueOf(instancia.getId());
        log.info("✅ Alerta SUPERVISION enviada a {} para reporte {}", supervisor.getNombreCompleto(), nombreReporte);
        
        return true;
    }

    private boolean existeAlertaHoy(InstanciaReporte instancia, Usuario usuario, String tipoNombre) {
        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = inicioHoy.plusDays(1);
        
        return alertaRepository.existsByInstanciaAndUsuarioDestinoAndTipoNombreAndFechaEnviadaBetween(
                instancia, usuario, tipoNombre, inicioHoy, finHoy);
    }

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
