package com.example.demo.service.impl;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.service.EmailNotificationService;
import com.example.demo.service.GeneradorInstanciasService;
import com.example.demo.service.ReporteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReporteServiceImpl implements ReporteService {

    private final ReporteRepository repository;
    private final EntidadRepository entidadRepo;
    private final FrecuenciaRepository frecuenciaRepo;
    private final GeneradorInstanciasService generadorInstancias;
    private final EmailNotificationService emailService;

    public ReporteServiceImpl(ReporteRepository repository,
                              EntidadRepository entidadRepo,
                              FrecuenciaRepository frecuenciaRepo,
                              GeneradorInstanciasService generadorInstancias,
                              EmailNotificationService emailService) {
        this.repository = repository;
        this.entidadRepo = entidadRepo;
        this.frecuenciaRepo = frecuenciaRepo;
        this.generadorInstancias = generadorInstancias;
        this.emailService = emailService;
    }

    @Override
    public List<Reporte> listar() {
        return repository.findAll();
    }

    @Override
    public Reporte obtenerPorId(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado"));
    }

    @Override
    @Transactional
    public Reporte crear(Reporte reporte) {
        // Guardar el reporte
        Reporte reporteGuardado = repository.save(reporte);
        
        // Generar instancias automáticamente para el año actual y siguiente
        if (reporteGuardado.isActivo()) {
            List<InstanciaReporte> instancias = generadorInstancias.generarInstanciasAnuales(reporteGuardado);
            System.out.println("✓ Se generaron " + instancias.size() + " instancias para el reporte " + reporteGuardado.getId());
        }
        
        // 📧 Enviar correo de asignación
        enviarCorreoAsignacion(reporteGuardado, false);
        
        return reporteGuardado;
    }

    @Override
    @Transactional
    public Reporte actualizar(String id, Reporte reporte) {
        Reporte existente = obtenerPorId(id);
        
        // Guardar responsable anterior para detectar reasignación
        Usuario responsableAnterior = existente.getResponsableElaboracion();
        boolean cambioResponsable = responsableAnterior != null && 
                reporte.getResponsableElaboracion() != null &&
                !responsableAnterior.getId().equals(reporte.getResponsableElaboracion().getId());
        
        // Detectar si es nueva asignación (no había responsable antes)
        boolean nuevaAsignacion = responsableAnterior == null && 
                reporte.getResponsableElaboracion() != null;
        
        boolean cambioFrecuencia = !existente.getFrecuencia().getId().equals(reporte.getFrecuencia().getId());
        boolean cambioDia = !java.util.Objects.equals(existente.getDiaVencimiento(), reporte.getDiaVencimiento());
        boolean cambioMes = !java.util.Objects.equals(existente.getMesVencimiento(), reporte.getMesVencimiento());
        boolean cambioFechaInicio = !java.util.Objects.equals(existente.getFechaInicioVigencia(), reporte.getFechaInicioVigencia());
        boolean cambioFechaFin = !java.util.Objects.equals(existente.getFechaFinVigencia(), reporte.getFechaFinVigencia());
        boolean cambioActivo = existente.isActivo() != reporte.isActivo();

        existente.setNombre(reporte.getNombre());
        existente.setEntidad(reporte.getEntidad());
        existente.setBaseLegal(reporte.getBaseLegal());
        existente.setFechaInicioVigencia(reporte.getFechaInicioVigencia());
        existente.setFechaFinVigencia(reporte.getFechaFinVigencia());
        // Asegurar que asociamos la entidad Frecuencia MANAGED y completa (no la que viene del request con sólo id)
        if (reporte.getFrecuencia() != null && reporte.getFrecuencia().getId() != null) {
            var frec = frecuenciaRepo.findById(reporte.getFrecuencia().getId())
                .orElseThrow(() -> new RuntimeException("Frecuencia no encontrada"));
            existente.setFrecuencia(frec);
        }
        existente.setDiaVencimiento(reporte.getDiaVencimiento());
        existente.setMesVencimiento(reporte.getMesVencimiento());
        existente.setPlazoAdicionalDias(reporte.getPlazoAdicionalDias());
        existente.setFormatoRequerido(reporte.getFormatoRequerido());
        existente.setLinkInstrucciones(reporte.getLinkInstrucciones());
        existente.setResponsableElaboracion(reporte.getResponsableElaboracion());
        existente.setResponsableSupervision(reporte.getResponsableSupervision());
        existente.setActivo(reporte.isActivo());

        Reporte reporteActualizado = repository.save(existente);
        
        // Si hay cambios que afectan el calendario de instancias, regenerar instancias futuras
        if (cambioFrecuencia || cambioDia || cambioMes || cambioFechaInicio || cambioFechaFin || cambioActivo) {
            System.out.println("⚠ Cambios en configuración del reporte detectados. Regenerando instancias futuras...");
            try {
                generadorInstancias.regenerarInstanciasFuturas(reporteActualizado);
            } catch (Exception e) {
                System.err.println("⚠️ Error al regenerar instancias: " + e.getMessage());
            }
        }

        // 📧 Enviar correo si cambió o se asignó responsable
        if (cambioResponsable || nuevaAsignacion) {
            enviarCorreoAsignacion(reporteActualizado, cambioResponsable);
            
            // Si es reasignación, notificar al responsable anterior
            if (cambioResponsable && responsableAnterior != null) {
                enviarCorreoDesasignacion(responsableAnterior, reporteActualizado);
            }
        }

        return reporteActualizado;
    }

    @Override
    public void eliminar(String id) {
        repository.deleteById(id);
    }

    @Override
    public List<Reporte> listarPorEntidad(Integer entidadId) {
        Entidad e = entidadRepo.findById(entidadId)
                .orElseThrow(() -> new RuntimeException("Entidad no encontrada"));

        return repository.findByEntidad(e);
    }

    @Override
    public List<Reporte> listarPorFrecuencia(Integer frecuenciaId) {
        Frecuencia f = frecuenciaRepo.findById(frecuenciaId)
                .orElseThrow(() -> new RuntimeException("Frecuencia no encontrada"));

        return repository.findByFrecuencia(f);
    }

    /**
     * Envía correo de asignación al responsable de elaboración y supervisor
     */
    private void enviarCorreoAsignacion(Reporte reporte, boolean esReasignacion) {
        String entidadNombre = reporte.getEntidad() != null ? reporte.getEntidad().getRazonSocial() : "N/A";
        String periodicidad = reporte.getFrecuencia() != null ? reporte.getFrecuencia().getNombre() : "N/A";
        
        // Correo al responsable de elaboración
        Usuario responsable = reporte.getResponsableElaboracion();
        if (responsable != null && responsable.getCorreo() != null) {
            String titulo = esReasignacion ? "Reporte Reasignado" : "Nuevo Reporte Asignado";
            String asunto = "📋 " + titulo + " - " + reporte.getNombre();
            String mensaje = String.format(
                    "Se te ha %s el reporte '%s'.\n\n" +
                    "Detalles:\n" +
                    "• Entidad: %s\n" +
                    "• Periodicidad: %s\n\n" +
                    "Ingresa al sistema para más detalles.",
                    esReasignacion ? "reasignado" : "asignado",
                    reporte.getNombre(),
                    entidadNombre,
                    periodicidad
            );
            
            try {
                emailService.enviarAlerta(responsable, asunto, mensaje, "Asignación", "Verde");
                System.out.println("📧 Correo de asignación enviado a: " + responsable.getNombreCompleto());
            } catch (Exception e) {
                System.err.println("⚠️ Error enviando correo de asignación: " + e.getMessage());
            }
        }
        
        // Correo al supervisor
        Usuario supervisor = reporte.getResponsableSupervision();
        if (supervisor != null && supervisor.getCorreo() != null && 
            (responsable == null || !supervisor.getId().equals(responsable.getId()))) {
            String asunto = "📋 Nuevo Reporte para Supervisar - " + reporte.getNombre();
            String mensaje = String.format(
                    "Se ha asignado el reporte '%s' a %s. Quedas como supervisor.\n\n" +
                    "Detalles:\n" +
                    "• Entidad: %s\n" +
                    "• Periodicidad: %s\n\n" +
                    "Ingresa al sistema para más detalles.",
                    reporte.getNombre(),
                    responsable != null ? responsable.getNombreCompleto() : "N/A",
                    entidadNombre,
                    periodicidad
            );
            
            try {
                emailService.enviarAlerta(supervisor, asunto, mensaje, "Supervisión", "Verde");
                System.out.println("📧 Correo de supervisión enviado a: " + supervisor.getNombreCompleto());
            } catch (Exception e) {
                System.err.println("⚠️ Error enviando correo de supervisión: " + e.getMessage());
            }
        }
    }

    /**
     * Envía correo al responsable anterior cuando se reasigna el reporte
     */
    private void enviarCorreoDesasignacion(Usuario responsableAnterior, Reporte reporte) {
        if (responsableAnterior == null || responsableAnterior.getCorreo() == null) return;
        
        String asunto = "📋 Reporte Reasignado - " + reporte.getNombre();
        String mensaje = String.format(
                "El reporte '%s' ha sido reasignado a otro usuario.\n\n" +
                "Ya no eres responsable de este reporte.",
                reporte.getNombre()
        );
        
        try {
            emailService.enviarAlerta(responsableAnterior, asunto, mensaje, "Reasignación", "Amarilla");
            System.out.println("📧 Correo de desasignación enviado a: " + responsableAnterior.getNombreCompleto());
        } catch (Exception e) {
            System.err.println("⚠️ Error enviando correo de desasignación: " + e.getMessage());
        }
    }
}