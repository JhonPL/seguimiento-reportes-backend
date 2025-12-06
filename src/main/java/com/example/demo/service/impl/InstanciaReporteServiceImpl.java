package com.example.demo.service.impl;

import com.example.demo.dto.InstanciaReporteDTO;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.service.FechaVencimientoCalculator;
import com.example.demo.service.GoogleDriveService;
import com.example.demo.service.InstanciaReporteService;
import com.example.demo.service.NotificacionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InstanciaReporteServiceImpl implements InstanciaReporteService {

    private final InstanciaReporteRepository repository;
    private final ReporteRepository reporteRepo;
    private final UsuarioRepository usuarioRepo;
    private final EstadoCumplimientoRepository estadoRepo;
    private final FechaVencimientoCalculator fechaCalculator;
    private final NotificacionService notificacionService;
    private final GoogleDriveService driveService;

    public InstanciaReporteServiceImpl(InstanciaReporteRepository repository,
            ReporteRepository reporteRepo,
            UsuarioRepository usuarioRepo,
            EstadoCumplimientoRepository estadoRepo,
            FechaVencimientoCalculator fechaCalculator,
            NotificacionService notificacionService,
            GoogleDriveService driveService) {
        this.repository = repository;
        this.reporteRepo = reporteRepo;
        this.usuarioRepo = usuarioRepo;
        this.estadoRepo = estadoRepo;
        this.fechaCalculator = fechaCalculator;
        this.notificacionService = notificacionService;
        this.driveService = driveService;
    }

    @Override
    public List<InstanciaReporte> listar() {
        return repository.findAll();
    }

    @Override
    public List<InstanciaReporteDTO> listarDTO() {
        return repository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    public InstanciaReporte obtenerPorId(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Instancia no encontrada"));
    }

    @Override
    public InstanciaReporteDTO obtenerDTOPorId(Integer id) {
        return convertirADTO(obtenerPorId(id));
    }

    @Override
    @Transactional
    public InstanciaReporte crear(InstanciaReporte instancia) {
        try {
            var fechaVencimiento = fechaCalculator.calcularFechaVencimiento(
                    instancia.getReporte(),
                    instancia.getPeriodoReportado());
            instancia.setFechaVencimientoCalculada(fechaVencimiento);
        } catch (Exception e) {
            instancia.setFechaVencimientoCalculada(LocalDate.now().plusMonths(1));
        }

        InstanciaReporte guardada = repository.save(instancia);
        System.out.println("✓ Instancia creada: " + guardada.getId() +
                " - Vence: " + guardada.getFechaVencimientoCalculada());
        return guardada;
    }

    @Override
    @Transactional
    public InstanciaReporte actualizar(Integer id, InstanciaReporte instancia) {
        InstanciaReporte existente = obtenerPorId(id);
        String estadoAnterior = existente.getEstado() != null ? existente.getEstado().getNombre() : "Pendiente";

        // ========== VALIDACIÓN DE INMUTABILIDAD ==========
        // Si la instancia ya fue enviada, NO permitir modificaciones
        if (esInstanciaEnviada(existente)) {
            throw new RuntimeException("No se puede modificar una instancia que ya fue enviada. " +
                    "Los reportes enviados son inmutables para garantizar la trazabilidad.");
        }
        // ==================================================

        existente.setPeriodoReportado(instancia.getPeriodoReportado());
        existente.setEstado(instancia.getEstado());
        existente.setLinkReporteFinal(instancia.getLinkReporteFinal());
        existente.setLinkEvidenciaEnvio(instancia.getLinkEvidenciaEnvio());
        existente.setObservaciones(instancia.getObservaciones());

        if (instancia.getFechaEnvioReal() != null) {
            existente.setFechaEnvioReal(instancia.getFechaEnvioReal());
            int diasDesviacion = fechaCalculator.calcularDiasDesviacion(
                    instancia.getFechaEnvioReal().toLocalDate(),
                    existente.getFechaVencimientoCalculada());
            existente.setDiasDesviacion(diasDesviacion);
        }

        InstanciaReporte actualizada = repository.save(existente);

        if (instancia.getEstado() != null && !estadoAnterior.equals(instancia.getEstado().getNombre())) {
            try {
                notificacionService.enviarNotificacionCambioEstado(actualizada, estadoAnterior);
            } catch (Exception e) {
                System.err.println("⚠️ Error al enviar notificación: " + e.getMessage());
            }
        }

        System.out.println("✓ Instancia actualizada: " + actualizada.getId());
        return actualizada;
    }

    /**
     * Verifica si una instancia ya fue enviada (inmutable)
     */
    private boolean esInstanciaEnviada(InstanciaReporte instancia) {
        if (instancia.getEstado() == null) return false;
        String estado = instancia.getEstado().getNombre().toUpperCase();
        return estado.contains("ENVIADO") || estado.contains("APROBADO");
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        InstanciaReporte existente = obtenerPorId(id);
        
        // ========== VALIDACIÓN DE INMUTABILIDAD ==========
        // Si la instancia ya fue enviada, NO permitir eliminación
        if (esInstanciaEnviada(existente)) {
            throw new RuntimeException("No se puede eliminar una instancia que ya fue enviada. " +
                    "Los reportes enviados son inmutables para garantizar la trazabilidad.");
        }
        // ==================================================
        
        repository.deleteById(id);
        System.out.println("✓ Instancia eliminada: " + id);
    }

    @Override
    public List<InstanciaReporte> listarPorReporte(String reporteId) {
        Reporte r = reporteRepo.findById(reporteId)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado"));
        return repository.findByReporte(r);
    }

    @Override
    public List<InstanciaReporteDTO> listarDTOPorReporte(String reporteId) {
        return listarPorReporte(reporteId).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InstanciaReporteDTO enviarReporte(Integer id, MultipartFile archivo, String observaciones,
            String linkEvidencia, Authentication authentication) throws IOException {

        InstanciaReporte instancia = obtenerPorId(id);
        Usuario usuario = obtenerUsuarioAutenticado(authentication);

        // Subir archivo a Drive
        Map<String, String> resultadoDrive = driveService.uploadFile(
                archivo,
                instancia.getReporte().getId(),
                instancia.getPeriodoReportado());

        // Actualizar campos
        instancia.setLinkReporteFinal(resultadoDrive.get("webViewLink"));
        instancia.setNombreArchivo(resultadoDrive.get("fileName"));
        instancia.setDriveFileId(resultadoDrive.get("fileId"));
        instancia.setLinkEvidenciaEnvio(linkEvidencia);
        instancia.setObservaciones(observaciones);
        instancia.setFechaEnvioReal(LocalDateTime.now());
        instancia.setEnviadoPor(usuario);

        // Calcular días de desviación
        int diasDesviacion = fechaCalculator.calcularDiasDesviacion(
                LocalDate.now(),
                instancia.getFechaVencimientoCalculada());
        instancia.setDiasDesviacion(diasDesviacion);

        // Seleccionar estado según desviación
        EstadoCumplimiento estadoEnviado;
        if (diasDesviacion <= 0) {
            estadoEnviado = estadoRepo.findByNombre("Enviado a tiempo")
                    .orElseThrow(() -> new RuntimeException("Estado 'Enviado a tiempo' no encontrado"));
        } else {
            estadoEnviado = estadoRepo.findByNombre("Enviado tarde")
                    .orElseThrow(() -> new RuntimeException("Estado 'Enviado tarde' no encontrado"));
        }
        instancia.setEstado(estadoEnviado);

        InstanciaReporte actualizada = repository.save(instancia);

        // Notificación
        try {
            notificacionService.enviarNotificacionCambioEstado(actualizada, estadoEnviado.getNombre());
        } catch (Exception e) {
            System.err.println("⚠️ Error al enviar notificación: " + e.getMessage());
        }

        return convertirADTO(actualizada);
    }

    @Override
    @Transactional
    public InstanciaReporteDTO enviarReporteConLink(
            Integer id,
            String linkReporte,
            String observaciones,
            String linkEvidencia,
            Authentication authentication) {

        InstanciaReporte instancia = obtenerPorId(id);
        Usuario usuario = obtenerUsuarioAutenticado(authentication);

        // 1. GUARDAR INFORMACIÓN DEL LINK ENVIADO
        instancia.setLinkReporteFinal(linkReporte);
        instancia.setLinkEvidenciaEnvio(linkEvidencia);
        instancia.setObservaciones(observaciones);
        instancia.setFechaEnvioReal(LocalDateTime.now());
        instancia.setEnviadoPor(usuario);

        // Como no se sube archivo, dejamos estos campos en null o como "externo"
        instancia.setDriveFileId(null);
        instancia.setNombreArchivo("Enviado mediante link");

        // 2. CALCULAR DÍAS DE DESVIACIÓN
        int diasDesviacion = fechaCalculator.calcularDiasDesviacion(
                LocalDate.now(),
                instancia.getFechaVencimientoCalculada());
        instancia.setDiasDesviacion(diasDesviacion);

        // 3. ASIGNAR ESTADO
        EstadoCumplimiento estadoEnviado;

        if (diasDesviacion <= 0) {
            estadoEnviado = estadoRepo.findByNombre("Enviado a tiempo")
                    .orElseThrow(() -> new RuntimeException("Estado 'Enviado a tiempo' no encontrado"));
        } else {
            estadoEnviado = estadoRepo.findByNombre("Enviado tarde")
                    .orElseThrow(() -> new RuntimeException("Estado 'Enviado tarde' no encontrado"));
        }

        String estadoAnterior = instancia.getEstado() != null
                ? instancia.getEstado().getNombre()
                : "Pendiente";

        instancia.setEstado(estadoEnviado);

        // 4. GUARDAR CAMBIOS
        InstanciaReporte actualizada = repository.save(instancia);

        // 5. NOTIFICACIÓN
        try {
            notificacionService.enviarNotificacionCambioEstado(
                    actualizada,
                    estadoAnterior);
        } catch (Exception e) {
            System.err.println("⚠️ Error al enviar notificación: " + e.getMessage());
        }

        System.out.println("✓ Reporte enviado mediante link: " + instancia.getReporte().getId()
                + " - Periodo: " + instancia.getPeriodoReportado());

        return convertirADTO(actualizada);
    }

    @Override
    public List<InstanciaReporteDTO> listarPendientes() {
        return repository.findAll().stream()
                .filter(i -> {
                    String estado = i.getEstado().getNombre().toUpperCase();

                    boolean esPendiente = estado.contains("PENDIENTE") || estado.contains("EN PROCESO");
                    boolean esEnviado = estado.contains("ENVIADO A TIEMPO") || estado.contains("ENVIADO TARDE");

                    return esPendiente && !esEnviado;

                })
                .map(this::convertirADTO)
                .sorted((a, b) -> {
                    if (a.getFechaVencimientoCalculada() == null)
                        return 1;
                    if (b.getFechaVencimientoCalculada() == null)
                        return -1;
                    return a.getFechaVencimientoCalculada().compareTo(b.getFechaVencimientoCalculada());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<InstanciaReporteDTO> listarVencidos() {
        LocalDate hoy = LocalDate.now();

        return repository.findAll().stream()
                .filter(i -> {
                    String estado = i.getEstado().getNombre().toUpperCase();

                    // Si ya fue enviado → NO es vencido
                    boolean yaEnviado = estado.contains("ENVIADO A TIEMPO") ||
                            estado.contains("ENVIADO TARDE") ||
                            estado.contains("APROBADO");

                    if (yaEnviado)
                        return false;

                    // Fecha vencida
                    boolean vencido = i.getFechaVencimientoCalculada() != null &&
                            i.getFechaVencimientoCalculada().isBefore(hoy);

                    return vencido;
                })
                .map(this::convertirADTO)
                .sorted((a, b) -> {
                    if (a.getFechaVencimientoCalculada() == null)
                        return 1;
                    if (b.getFechaVencimientoCalculada() == null)
                        return -1;
                    return a.getFechaVencimientoCalculada().compareTo(b.getFechaVencimientoCalculada());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<InstanciaReporteDTO> listarHistorico(String reporteId, Integer entidadId, Integer year, Integer mes, Authentication authentication) {
        // Obtener usuario autenticado y su rol
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        String rol = usuario.getRol() != null ? usuario.getRol().getNombre().toUpperCase() : "";
        
        return repository.findAll().stream()
                .filter(i -> {
                    String estado = i.getEstado().getNombre().toUpperCase();
                    return estado.contains("ENVIADO A TIEMPO")
                            || estado.contains("ENVIADO TARDE")
                            || estado.contains("APROBADO");

                })
                // Filtro por rol
                .filter(i -> {
                    if (rol.contains("ADMIN")) {
                        // Admin ve todo
                        return true;
                    } else if (rol.contains("SUPERVISOR") || rol.contains("SUPERV")) {
                        // Supervisor ve solo los reportes de sus supervisados
                        return i.getReporte() != null 
                                && i.getReporte().getResponsableSupervision() != null
                                && i.getReporte().getResponsableSupervision().getId().equals(usuario.getId());
                    } else {
                        // Responsable ve solo sus reportes
                        return i.getReporte() != null 
                                && i.getReporte().getResponsableElaboracion() != null
                                && i.getReporte().getResponsableElaboracion().getId().equals(usuario.getId());
                    }
                })
                .filter(i -> reporteId == null || i.getReporte().getId().equals(reporteId))
                .filter(i -> entidadId == null || i.getReporte().getEntidad().getId().equals(entidadId))
                .filter(i -> {
                    if (year == null)
                        return true;
                    if (i.getFechaEnvioReal() == null)
                        return false;
                    return i.getFechaEnvioReal().getYear() == year;
                })
                .filter(i -> {
                    if (mes == null)
                        return true;
                    if (i.getFechaEnvioReal() == null)
                        return false;
                    return i.getFechaEnvioReal().getMonthValue() == mes;
                })
                .map(this::convertirADTO)
                .sorted((a, b) -> {
                    if (a.getFechaEnvioReal() == null)
                        return 1;
                    if (b.getFechaEnvioReal() == null)
                        return -1;
                    return b.getFechaEnvioReal().compareTo(a.getFechaEnvioReal()); // Más recientes primero
                })
                .collect(Collectors.toList());
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Usuario obtenerUsuarioAutenticado(Authentication authentication) {
        String correo = authentication.getName();
        return usuarioRepo.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private InstanciaReporteDTO convertirADTO(InstanciaReporte instancia) {
        InstanciaReporteDTO dto = new InstanciaReporteDTO();

        dto.setId(instancia.getId());

        // Datos del reporte
        Reporte reporte = instancia.getReporte();
        dto.setReporteId(reporte.getId());
        dto.setReporteNombre(reporte.getNombre());
        dto.setEntidadNombre(reporte.getEntidad().getRazonSocial());
        dto.setEntidadId(reporte.getEntidad().getId());
        dto.setFrecuencia(reporte.getFrecuencia().getNombre());
        dto.setFormatoRequerido(reporte.getFormatoRequerido());
        dto.setBaseLegal(reporte.getBaseLegal());

        // Responsables
        if (reporte.getResponsableElaboracion() != null) {
            dto.setResponsableElaboracion(reporte.getResponsableElaboracion().getNombreCompleto());
            dto.setResponsableElaboracionId(reporte.getResponsableElaboracion().getId());
        }
        if (reporte.getResponsableSupervision() != null) {
            dto.setResponsableSupervision(reporte.getResponsableSupervision().getNombreCompleto());
            dto.setResponsableSupervisionId(reporte.getResponsableSupervision().getId());
        }

        // Datos de la instancia
        dto.setPeriodoReportado(instancia.getPeriodoReportado());
        dto.setFechaVencimientoCalculada(instancia.getFechaVencimientoCalculada());
        dto.setFechaEnvioReal(instancia.getFechaEnvioReal());
        dto.setEstadoNombre(instancia.getEstado().getNombre());
        dto.setEstadoId(instancia.getEstado().getId());
        dto.setDiasDesviacion(instancia.getDiasDesviacion());

        // Calcular días hasta vencimiento
        if (instancia.getFechaVencimientoCalculada() != null) {
            long dias = ChronoUnit.DAYS.between(LocalDate.now(), instancia.getFechaVencimientoCalculada());
            dto.setDiasHastaVencimiento((int) dias);
        }

        // Archivos y links
        dto.setLinkReporteFinal(instancia.getLinkReporteFinal());
        dto.setLinkEvidenciaEnvio(instancia.getLinkEvidenciaEnvio());
        dto.setNombreArchivo(instancia.getNombreArchivo());
        dto.setDriveFileId(instancia.getDriveFileId());

        // Usuario que envió
        if (instancia.getEnviadoPor() != null) {
            dto.setEnviadoPorNombre(instancia.getEnviadoPor().getNombreCompleto());
            dto.setEnviadoPorId(instancia.getEnviadoPor().getId());
        }

        // Otros
        dto.setObservaciones(instancia.getObservaciones());
        dto.setFechaCreacion(instancia.getFechaCreacion());
        dto.setFechaActualizacion(instancia.getFechaActualizacion());

        // Estados calculados
        String estado = instancia.getEstado().getNombre().toUpperCase();
        dto.setEnviado(
                estado.contains("ENVIADO A TIEMPO") ||
                        estado.contains("ENVIADO TARDE") ||
                        estado.contains("APROBADO"));

        if (instancia.getFechaVencimientoCalculada() != null) {
            dto.setVencido(!dto.isEnviado() && instancia.getFechaVencimientoCalculada().isBefore(LocalDate.now()));

            // Prioridad
            long dias = ChronoUnit.DAYS.between(LocalDate.now(), instancia.getFechaVencimientoCalculada());
            if (dto.isEnviado()) {
                dto.setPrioridad("BAJA");
            } else if (dias < 0) {
                dto.setPrioridad("CRITICA");
            } else if (dias <= 3) {
                dto.setPrioridad("ALTA");
            } else if (dias <= 7) {
                dto.setPrioridad("MEDIA");
            } else {
                dto.setPrioridad("BAJA");
            }
        } else {
            dto.setPrioridad("MEDIA");
        }

        // ========== CAMPOS DE CORRECCIÓN ==========
        dto.setTieneCorreccion(instancia.getTieneCorreccion() != null && instancia.getTieneCorreccion());
        dto.setLinkCorreccion(instancia.getLinkCorreccion());
        dto.setDriveFileIdCorreccion(instancia.getDriveFileIdCorreccion());
        dto.setNombreArchivoCorreccion(instancia.getNombreArchivoCorreccion());
        dto.setMotivoCorreccion(instancia.getMotivoCorreccion());
        dto.setFechaCorreccion(instancia.getFechaCorreccion());
        
        if (instancia.getCorregidoPor() != null) {
            dto.setCorregidoPorNombre(instancia.getCorregidoPor().getNombreCompleto());
            dto.setCorregidoPorId(instancia.getCorregidoPor().getId());
        }
        
        // Solo se puede corregir si ya fue enviado
        dto.setPuedeCorregir(dto.isEnviado());
        // ==================================================

        return dto;
    }

    // ==================== MÉTODOS DE CORRECCIÓN ====================

    @Override
    @Transactional
    public InstanciaReporteDTO corregirReporte(Integer id, MultipartFile archivo, String motivo,
                                                Authentication authentication) throws IOException {
        
        InstanciaReporte instancia = obtenerPorId(id);
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        
        // Validar que sea administrador
        validarEsAdministrador(usuario);
        
        // Validar que la instancia ya fue enviada
        if (!esInstanciaEnviada(instancia)) {
            throw new RuntimeException("Solo se pueden corregir reportes que ya fueron enviados. " +
                    "Si el reporte aún no se envía, puede editarlo normalmente.");
        }
        
        // Validar que se proporcione un motivo
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new RuntimeException("Debe proporcionar un motivo para la corrección.");
        }

        // Subir archivo de corrección a Drive (el original NO se toca)
        Map<String, String> resultadoDrive = driveService.uploadFile(
                archivo,
                instancia.getReporte().getId() + "_CORRECCION",
                instancia.getPeriodoReportado());

        // Actualizar campos de corrección
        instancia.setTieneCorreccion(true);
        instancia.setLinkCorreccion(resultadoDrive.get("webViewLink"));
        instancia.setDriveFileIdCorreccion(resultadoDrive.get("fileId"));
        instancia.setNombreArchivoCorreccion(resultadoDrive.get("fileName"));
        instancia.setMotivoCorreccion(motivo);
        instancia.setFechaCorreccion(LocalDateTime.now());
        instancia.setCorregidoPor(usuario);

        InstanciaReporte actualizada = repository.save(instancia);

        System.out.println("✓ Corrección agregada a instancia " + id + 
                " por " + usuario.getNombreCompleto() + 
                ". Motivo: " + motivo);

        return convertirADTO(actualizada);
    }

    @Override
    @Transactional
    public InstanciaReporteDTO corregirReporteConLink(Integer id, String linkCorreccion, String motivo,
                                                       Authentication authentication) {
        
        InstanciaReporte instancia = obtenerPorId(id);
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        
        // Validar que sea administrador
        validarEsAdministrador(usuario);
        
        // Validar que la instancia ya fue enviada
        if (!esInstanciaEnviada(instancia)) {
            throw new RuntimeException("Solo se pueden corregir reportes que ya fueron enviados.");
        }
        
        // Validar que se proporcione un motivo
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new RuntimeException("Debe proporcionar un motivo para la corrección.");
        }
        
        // Validar que se proporcione un link
        if (linkCorreccion == null || linkCorreccion.trim().isEmpty()) {
            throw new RuntimeException("Debe proporcionar el link de la corrección.");
        }

        // Actualizar campos de corrección
        instancia.setTieneCorreccion(true);
        instancia.setLinkCorreccion(linkCorreccion);
        instancia.setDriveFileIdCorreccion(null);
        instancia.setNombreArchivoCorreccion("Corrección mediante link");
        instancia.setMotivoCorreccion(motivo);
        instancia.setFechaCorreccion(LocalDateTime.now());
        instancia.setCorregidoPor(usuario);

        InstanciaReporte actualizada = repository.save(instancia);

        System.out.println("✓ Corrección (link) agregada a instancia " + id + 
                " por " + usuario.getNombreCompleto());

        return convertirADTO(actualizada);
    }

    /**
     * Valida que el usuario sea administrador
     */
    private void validarEsAdministrador(Usuario usuario) {
        if (usuario.getRol() == null) {
            throw new RuntimeException("Usuario sin rol asignado.");
        }
        String rol = usuario.getRol().getNombre().toUpperCase();
        if (!rol.contains("ADMIN")) {
            throw new RuntimeException("Solo los administradores pueden realizar correcciones a reportes enviados.");
        }
    }
}