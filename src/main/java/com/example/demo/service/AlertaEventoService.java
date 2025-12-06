package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.AlertaRepository;
import com.example.demo.repository.TipoAlertaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Servicio para enviar alertas por eventos específicos:
 * - Asignación de reporte
 * - Envío de reporte
 */
@Service
public class AlertaEventoService {

    private static final Logger log = LoggerFactory.getLogger(AlertaEventoService.class);

    private final AlertaRepository alertaRepository;
    private final TipoAlertaRepository tipoAlertaRepository;
    private final EmailNotificationService emailService;

    public AlertaEventoService(AlertaRepository alertaRepository,
                               TipoAlertaRepository tipoAlertaRepository,
                               EmailNotificationService emailService) {
        this.alertaRepository = alertaRepository;
        this.tipoAlertaRepository = tipoAlertaRepository;
        this.emailService = emailService;
    }

    // ==================== ALERTAS DE ASIGNACIÓN ====================

    /**
     * Notificar al responsable que se le asignó un nuevo reporte
     */
    @Transactional
    public void notificarAsignacionResponsable(InstanciaReporte instancia) {
        if (instancia == null || instancia.getReporte() == null) {
            log.warn("No se puede notificar asignación: instancia o reporte nulo");
            return;
        }

        Reporte reporte = instancia.getReporte();
        Usuario responsable = reporte.getResponsableElaboracion();

        if (responsable == null) {
            log.warn("No se puede notificar asignación: responsable no asignado");
            return;
        }

        String nombreReporte = reporte.getNombre();
        String entidad = reporte.getEntidad() != null ? reporte.getEntidad().getRazonSocial() : "";
        String fechaVencimiento = instancia.getFechaVencimientoCalculada() != null 
                ? instancia.getFechaVencimientoCalculada().toString() : "Por definir";

        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Se le ha asignado un nuevo reporte para elaboración:\n\n");
        mensaje.append("<strong>Reporte:</strong> ").append(nombreReporte).append("\n");
        if (!entidad.isEmpty()) {
            mensaje.append("<strong>Entidad:</strong> ").append(entidad).append("\n");
        }
        mensaje.append("<strong>Periodo:</strong> ").append(instancia.getPeriodoReportado()).append("\n");
        mensaje.append("<strong>Fecha de vencimiento:</strong> ").append(fechaVencimiento).append("\n\n");
        mensaje.append("Por favor, inicie la preparación del reporte a la brevedad.");

        // Guardar alerta en BD
        TipoAlerta tipoAlerta = obtenerOCrearTipoAlerta("ASIGNACION", "azul");
        Alerta alerta = crearAlerta(instancia, tipoAlerta, responsable, mensaje.toString());
        alertaRepository.save(alerta);

        // Enviar email
        String asunto = "Nueva asignación: " + nombreReporte + " - " + instancia.getPeriodoReportado();
        emailService.enviarAlerta(responsable, asunto, mensaje.toString(), "ASIGNACIÓN", "azul");

        log.info("Alerta de asignación enviada a {} para reporte {}", 
                responsable.getNombreCompleto(), nombreReporte);
    }

    /**
     * Notificar al supervisor que se asignó un nuevo reporte bajo su supervisión
     */
    @Transactional
    public void notificarAsignacionSupervisor(InstanciaReporte instancia) {
        if (instancia == null || instancia.getReporte() == null) {
            return;
        }

        Reporte reporte = instancia.getReporte();
        Usuario supervisor = reporte.getResponsableSupervision();
        Usuario responsable = reporte.getResponsableElaboracion();

        if (supervisor == null) {
            log.warn("No se puede notificar al supervisor: no está asignado");
            return;
        }

        String nombreReporte = reporte.getNombre();
        String nombreResponsable = responsable != null ? responsable.getNombreCompleto() : "Sin asignar";
        String entidad = reporte.getEntidad() != null ? reporte.getEntidad().getRazonSocial() : "";
        String fechaVencimiento = instancia.getFechaVencimientoCalculada() != null 
                ? instancia.getFechaVencimientoCalculada().toString() : "Por definir";

        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Se ha creado una nueva instancia de reporte bajo su supervisión:\n\n");
        mensaje.append("<strong>Reporte:</strong> ").append(nombreReporte).append("\n");
        mensaje.append("<strong>Responsable de elaboración:</strong> ").append(nombreResponsable).append("\n");
        if (!entidad.isEmpty()) {
            mensaje.append("<strong>Entidad:</strong> ").append(entidad).append("\n");
        }
        mensaje.append("<strong>Periodo:</strong> ").append(instancia.getPeriodoReportado()).append("\n");
        mensaje.append("<strong>Fecha de vencimiento:</strong> ").append(fechaVencimiento);

        // Guardar alerta en BD
        TipoAlerta tipoAlerta = obtenerOCrearTipoAlerta("ASIGNACION", "azul");
        Alerta alerta = crearAlerta(instancia, tipoAlerta, supervisor, mensaje.toString());
        alertaRepository.save(alerta);

        // Enviar email
        String asunto = "Nueva supervisión: " + nombreReporte + " - " + instancia.getPeriodoReportado();
        emailService.enviarAlerta(supervisor, asunto, mensaje.toString(), "ASIGNACIÓN", "azul");

        log.info("Alerta de asignación (supervisor) enviada a {} para reporte {}", 
                supervisor.getNombreCompleto(), nombreReporte);
    }

    /**
     * Método conveniente para notificar a ambos (responsable y supervisor)
     */
    @Transactional
    public void notificarNuevaAsignacion(InstanciaReporte instancia) {
        notificarAsignacionResponsable(instancia);
        notificarAsignacionSupervisor(instancia);
    }

    // ==================== ALERTAS DE ENVÍO ====================

    /**
     * Notificar al responsable que su reporte fue enviado exitosamente
     */
    @Transactional
    public void notificarEnvioExitosoResponsable(InstanciaReporte instancia) {
        if (instancia == null || instancia.getReporte() == null) {
            return;
        }

        Reporte reporte = instancia.getReporte();
        Usuario responsable = reporte.getResponsableElaboracion();

        if (responsable == null) {
            return;
        }

        String nombreReporte = reporte.getNombre();
        String entidad = reporte.getEntidad() != null ? reporte.getEntidad().getRazonSocial() : "";
        String fechaEnvio = instancia.getFechaEnvioReal() != null 
                ? instancia.getFechaEnvioReal().toString() : LocalDateTime.now().toString();

        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Su reporte ha sido enviado <strong>exitosamente</strong>.\n\n");
        mensaje.append("<strong>Reporte:</strong> ").append(nombreReporte).append("\n");
        if (!entidad.isEmpty()) {
            mensaje.append("<strong>Entidad:</strong> ").append(entidad).append("\n");
        }
        mensaje.append("<strong>Periodo:</strong> ").append(instancia.getPeriodoReportado()).append("\n");
        mensaje.append("<strong>Fecha de envío:</strong> ").append(fechaEnvio).append("\n\n");
        mensaje.append("Gracias por cumplir con la entrega.");

        // Guardar alerta en BD
        TipoAlerta tipoAlerta = obtenerOCrearTipoAlerta("ENVIO", "verde");
        Alerta alerta = crearAlerta(instancia, tipoAlerta, responsable, mensaje.toString());
        alertaRepository.save(alerta);

        // Enviar email
        String asunto = "Envío exitoso: " + nombreReporte + " - " + instancia.getPeriodoReportado();
        emailService.enviarAlerta(responsable, asunto, mensaje.toString(), "ENVÍO EXITOSO", "verde");

        log.info("Alerta de envío exitoso enviada a {} para reporte {}", 
                responsable.getNombreCompleto(), nombreReporte);
    }

    /**
     * Notificar al supervisor que un reporte fue enviado
     */
    @Transactional
    public void notificarEnvioExitosoSupervisor(InstanciaReporte instancia) {
        if (instancia == null || instancia.getReporte() == null) {
            return;
        }

        Reporte reporte = instancia.getReporte();
        Usuario supervisor = reporte.getResponsableSupervision();
        Usuario responsable = reporte.getResponsableElaboracion();

        if (supervisor == null) {
            return;
        }

        String nombreReporte = reporte.getNombre();
        String nombreResponsable = responsable != null ? responsable.getNombreCompleto() : "N/A";
        String entidad = reporte.getEntidad() != null ? reporte.getEntidad().getRazonSocial() : "";
        String fechaEnvio = instancia.getFechaEnvioReal() != null 
                ? instancia.getFechaEnvioReal().toString() : LocalDateTime.now().toString();
        
        // Calcular si fue a tiempo o con retraso
        String estadoEnvio = "A tiempo";
        if (instancia.getDiasDesviacion() != null && instancia.getDiasDesviacion() > 0) {
            estadoEnvio = "Con " + instancia.getDiasDesviacion() + " día(s) de retraso";
        } else if (instancia.getDiasDesviacion() != null && instancia.getDiasDesviacion() < 0) {
            estadoEnvio = "Anticipado por " + Math.abs(instancia.getDiasDesviacion()) + " día(s)";
        }

        StringBuilder mensaje = new StringBuilder();
        mensaje.append("El siguiente reporte ha sido <strong>enviado</strong>:\n\n");
        mensaje.append("<strong>Reporte:</strong> ").append(nombreReporte).append("\n");
        mensaje.append("<strong>Enviado por:</strong> ").append(nombreResponsable).append("\n");
        if (!entidad.isEmpty()) {
            mensaje.append("<strong>Entidad:</strong> ").append(entidad).append("\n");
        }
        mensaje.append("<strong>Periodo:</strong> ").append(instancia.getPeriodoReportado()).append("\n");
        mensaje.append("<strong>Fecha de envío:</strong> ").append(fechaEnvio).append("\n");
        mensaje.append("<strong>Estado:</strong> ").append(estadoEnvio);

        // Guardar alerta en BD
        TipoAlerta tipoAlerta = obtenerOCrearTipoAlerta("ENVIO", "verde");
        Alerta alerta = crearAlerta(instancia, tipoAlerta, supervisor, mensaje.toString());
        alertaRepository.save(alerta);

        // Enviar email
        String asunto = "Reporte enviado: " + nombreReporte + " - " + instancia.getPeriodoReportado();
        emailService.enviarAlerta(supervisor, asunto, mensaje.toString(), "ENVÍO EXITOSO", "verde");

        log.info("Alerta de envío (supervisor) enviada a {} para reporte {}", 
                supervisor.getNombreCompleto(), nombreReporte);
    }

    /**
     * Método conveniente para notificar a ambos (responsable y supervisor)
     */
    @Transactional
    public void notificarEnvioExitoso(InstanciaReporte instancia) {
        notificarEnvioExitosoResponsable(instancia);
        notificarEnvioExitosoSupervisor(instancia);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private TipoAlerta obtenerOCrearTipoAlerta(String nombre, String color) {
        Optional<TipoAlerta> existente = tipoAlertaRepository.findByNombre(nombre);
        
        if (existente.isPresent()) {
            return existente.get();
        }
        
        TipoAlerta nuevo = new TipoAlerta();
        nuevo.setNombre(nombre);
        nuevo.setColor(color);
        nuevo.setDiasAntesVencimiento(0);
        nuevo.setEsPostVencimiento(false);
        
        return tipoAlertaRepository.save(nuevo);
    }

    private Alerta crearAlerta(InstanciaReporte instancia, TipoAlerta tipo, Usuario destinatario, String mensaje) {
        Alerta alerta = new Alerta();
        alerta.setInstancia(instancia);
        alerta.setTipo(tipo);
        alerta.setUsuarioDestino(destinatario);
        alerta.setMensaje(mensaje);
        alerta.setFechaProgramada(LocalDateTime.now());
        alerta.setFechaEnviada(LocalDateTime.now());
        alerta.setEnviada(true);
        alerta.setLeida(false);
        return alerta;
    }
}
