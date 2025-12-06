package com.example.demo.service;

import com.example.demo.dto.EventoCalendarioDTO;
import com.example.demo.entity.InstanciaReporte;
import com.example.demo.entity.Usuario;
import com.example.demo.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CalendarioService {

    private final InstanciaReporteRepository instanciaRepo;
    private final UsuarioRepository usuarioRepo;

    public CalendarioService(InstanciaReporteRepository instanciaRepo,
                            UsuarioRepository usuarioRepo) {
        this.instanciaRepo = instanciaRepo;
        this.usuarioRepo = usuarioRepo;
    }

    public List<EventoCalendarioDTO> obtenerEventosCalendario(
            YearMonth mes, Integer entidadId, Integer responsableId, 
            String frecuencia, Authentication authentication) {
        
        LocalDate inicio = mes.atDay(1);
        LocalDate fin = mes.atEndOfMonth();

        List<InstanciaReporte> instancias = instanciaRepo.findByFechaVencimientoCalculadaBetween(inicio, fin);

        // Aplicar filtros con validaciones null
        if (entidadId != null) {
            instancias = instancias.stream()
                    .filter(i -> i.getReporte() != null && 
                               i.getReporte().getEntidad() != null &&
                               i.getReporte().getEntidad().getId().equals(entidadId))
                    .collect(Collectors.toList());
        }

        if (responsableId != null) {
            instancias = instancias.stream()
                    .filter(i -> i.getReporte() != null && 
                               i.getReporte().getResponsableElaboracion() != null &&
                               i.getReporte().getResponsableElaboracion().getId().equals(responsableId))
                    .collect(Collectors.toList());
        }

        if (frecuencia != null && !frecuencia.isEmpty()) {
            instancias = instancias.stream()
                    .filter(i -> i.getReporte() != null && 
                               i.getReporte().getFrecuencia() != null &&
                               i.getReporte().getFrecuencia().getNombre().equalsIgnoreCase(frecuencia))
                    .collect(Collectors.toList());
        }

        return instancias.stream()
                .filter(this::esInstanciaValida)
                .map(this::convertirAEvento)
                .collect(Collectors.toList());
    }

    public List<EventoCalendarioDTO> obtenerMiCalendario(YearMonth mes, Authentication authentication) {
        String correoUsuario = authentication.getName();
        Usuario usuario = usuarioRepo.findByCorreo(correoUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        LocalDate inicio = mes.atDay(1);
        LocalDate fin = mes.atEndOfMonth();

        String rol = usuario.getRol() != null ? usuario.getRol().getNombre().toUpperCase() : "";

        List<InstanciaReporte> instancias = instanciaRepo.findByFechaVencimientoCalculadaBetween(inicio, fin);

        if (rol.contains("ADMIN")) {
            // Admin ve todo
        } else if (rol.contains("SUPERVISOR")) {
            // Supervisor ve los que supervisa
            instancias = instancias.stream()
                    .filter(i -> i.getReporte() != null && 
                               i.getReporte().getResponsableSupervision() != null &&
                               i.getReporte().getResponsableSupervision().getId().equals(usuario.getId()))
                    .collect(Collectors.toList());
        } else {
            // Elaborador solo ve los suyos
            instancias = instancias.stream()
                    .filter(i -> i.getReporte() != null && 
                               i.getReporte().getResponsableElaboracion() != null &&
                               i.getReporte().getResponsableElaboracion().getId().equals(usuario.getId()))
                    .collect(Collectors.toList());
        }

        return instancias.stream()
                .filter(this::esInstanciaValida)
                .map(this::convertirAEvento)
                .collect(Collectors.toList());
    }

    public Map<String, List<EventoCalendarioDTO>> obtenerVistaAnual(int year, Authentication authentication) {
        Map<String, List<EventoCalendarioDTO>> vistaAnual = new LinkedHashMap<>();

        for (int mes = 1; mes <= 12; mes++) {
            YearMonth yearMonth = YearMonth.of(year, mes);
            List<EventoCalendarioDTO> eventos = obtenerMiCalendario(yearMonth, authentication);
            vistaAnual.put(yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")), eventos);
        }

        return vistaAnual;
    }

    public List<InstanciaReporte> buscarReportesAvanzada(
            Integer entidadId, LocalDate fechaInicio, LocalDate fechaFin,
            String periodoReportado, Integer estadoId,
            Integer responsableElaboracionId, Integer responsableSupervisionId,
            String proceso, String busquedaLibre) {

        List<InstanciaReporte> instancias = instanciaRepo.findAll();

        if (entidadId != null) {
            instancias = instancias.stream()
                    .filter(i -> i.getReporte() != null && 
                               i.getReporte().getEntidad() != null &&
                               i.getReporte().getEntidad().getId().equals(entidadId))
                    .collect(Collectors.toList());
        }

        if (fechaInicio != null && fechaFin != null) {
            instancias = instancias.stream()
                    .filter(i -> i.getFechaVencimientoCalculada() != null)
                    .filter(i -> !i.getFechaVencimientoCalculada().isBefore(fechaInicio) &&
                               !i.getFechaVencimientoCalculada().isAfter(fechaFin))
                    .collect(Collectors.toList());
        }

        if (periodoReportado != null && !periodoReportado.isEmpty()) {
            instancias = instancias.stream()
                    .filter(i -> i.getPeriodoReportado() != null &&
                               i.getPeriodoReportado().contains(periodoReportado))
                    .collect(Collectors.toList());
        }

        if (estadoId != null) {
            instancias = instancias.stream()
                    .filter(i -> i.getEstado() != null &&
                               i.getEstado().getId().equals(estadoId))
                    .collect(Collectors.toList());
        }

        if (responsableElaboracionId != null) {
            instancias = instancias.stream()
                    .filter(i -> i.getReporte() != null && 
                               i.getReporte().getResponsableElaboracion() != null &&
                               i.getReporte().getResponsableElaboracion().getId().equals(responsableElaboracionId))
                    .collect(Collectors.toList());
        }

        if (responsableSupervisionId != null) {
            instancias = instancias.stream()
                    .filter(i -> i.getReporte() != null && 
                               i.getReporte().getResponsableSupervision() != null &&
                               i.getReporte().getResponsableSupervision().getId().equals(responsableSupervisionId))
                    .collect(Collectors.toList());
        }

        if (proceso != null && !proceso.isEmpty()) {
            instancias = instancias.stream()
                    .filter(i -> i.getReporte() != null && 
                               i.getReporte().getResponsableElaboracion() != null &&
                               i.getReporte().getResponsableElaboracion().getProceso() != null &&
                               i.getReporte().getResponsableElaboracion().getProceso().toLowerCase()
                                    .contains(proceso.toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (busquedaLibre != null && !busquedaLibre.isEmpty()) {
            String busqueda = busquedaLibre.toLowerCase();
            instancias = instancias.stream()
                    .filter(i -> {
                        boolean coincide = false;
                        if (i.getReporte() != null && i.getReporte().getNombre() != null) {
                            coincide = coincide || i.getReporte().getNombre().toLowerCase().contains(busqueda);
                        }
                        if (i.getReporte() != null && i.getReporte().getEntidad() != null &&
                            i.getReporte().getEntidad().getRazonSocial() != null) {
                            coincide = coincide || i.getReporte().getEntidad().getRazonSocial().toLowerCase().contains(busqueda);
                        }
                        if (i.getPeriodoReportado() != null) {
                            coincide = coincide || i.getPeriodoReportado().toLowerCase().contains(busqueda);
                        }
                        if (i.getObservaciones() != null) {
                            coincide = coincide || i.getObservaciones().toLowerCase().contains(busqueda);
                        }
                        return coincide;
                    })
                    .collect(Collectors.toList());
        }

        return instancias;
    }

    /**
     * Valida que una instancia tenga los datos mínimos para convertirla a evento
     */
    private boolean esInstanciaValida(InstanciaReporte instancia) {
        return instancia != null &&
               instancia.getReporte() != null &&
               instancia.getReporte().getEntidad() != null &&
               instancia.getReporte().getFrecuencia() != null &&
               instancia.getEstado() != null &&
               instancia.getFechaVencimientoCalculada() != null;
    }

    private EventoCalendarioDTO convertirAEvento(InstanciaReporte instancia) {
        EventoCalendarioDTO evento = new EventoCalendarioDTO();
        
        evento.setId(instancia.getId());
        evento.setTitulo(instancia.getReporte().getNombre() + " - " + 
                        instancia.getReporte().getEntidad().getRazonSocial());
        evento.setFecha(instancia.getFechaVencimientoCalculada());
        evento.setStart(instancia.getFechaVencimientoCalculada().toString());
        evento.setEstado(instancia.getEstado().getNombre());
        evento.setEntidad(instancia.getReporte().getEntidad().getRazonSocial());
        
        // Responsable puede ser null
        if (instancia.getReporte().getResponsableElaboracion() != null) {
            evento.setResponsable(instancia.getReporte().getResponsableElaboracion().getNombreCompleto());
            evento.setResponsableElaboracionId(instancia.getReporte().getResponsableElaboracion().getId());
        } else {
            evento.setResponsable("Sin asignar");
        }
        
        evento.setFrecuencia(instancia.getReporte().getFrecuencia().getNombre());
        evento.setPeriodoReportado(instancia.getPeriodoReportado());
        evento.setReporteId(instancia.getReporte().getId());
        evento.setEntidadId(instancia.getReporte().getEntidad().getId());
        
        // Supervisor puede ser null
        if (instancia.getReporte().getResponsableSupervision() != null) {
            evento.setResponsableSupervisionId(instancia.getReporte().getResponsableSupervision().getId());
        }

        // Calcular días hasta vencimiento
        long diasHasta = ChronoUnit.DAYS.between(LocalDate.now(), instancia.getFechaVencimientoCalculada());
        evento.setDiasHastaVencimiento((int) diasHasta);

        // Determinar color y prioridad según estado y días
        String estado = instancia.getEstado().getNombre().toUpperCase();
        if (estado.contains("ENVIADO") || estado.contains("APROBADO")) {
            evento.setColor("#10B981"); // Verde
            evento.setBackgroundColor("#10B981");
            evento.setBorderColor("#059669");
            evento.setTextColor("#FFFFFF");
            evento.setPrioridad("BAJA");
        } else if (estado.contains("VENCIDO") || diasHasta < 0) {
            evento.setColor("#EF4444"); // Rojo
            evento.setBackgroundColor("#EF4444");
            evento.setBorderColor("#DC2626");
            evento.setTextColor("#FFFFFF");
            evento.setPrioridad("CRITICA");
        } else if (diasHasta <= 1) {
            evento.setColor("#F97316"); // Naranja
            evento.setBackgroundColor("#F97316");
            evento.setBorderColor("#EA580C");
            evento.setTextColor("#FFFFFF");
            evento.setPrioridad("ALTA");
        } else if (diasHasta <= 7) {
            evento.setColor("#F59E0B"); // Amarillo
            evento.setBackgroundColor("#F59E0B");
            evento.setBorderColor("#D97706");
            evento.setTextColor("#FFFFFF");
            evento.setPrioridad("MEDIA");
        } else {
            evento.setColor("#6B7280"); // Gris
            evento.setBackgroundColor("#6B7280");
            evento.setBorderColor("#4B5563");
            evento.setTextColor("#FFFFFF");
            evento.setPrioridad("BAJA");
        }

        return evento;
    }
}