package com.example.demo.service;

import com.example.demo.dto.EstadisticasDTO;
import com.example.demo.entity.InstanciaReporte;
import com.example.demo.repository.AlertaRepository;
import com.example.demo.repository.InstanciaReporteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EstadisticasService {

    private final InstanciaReporteRepository instanciaRepo;
    private final AlertaRepository alertaRepo;

    public EstadisticasService(InstanciaReporteRepository instanciaRepo,
            AlertaRepository alertaRepo) {
        this.instanciaRepo = instanciaRepo;
        this.alertaRepo = alertaRepo;
    }

    // ================= ADMIN - Ve todo =================

    public EstadisticasDTO obtenerEstadisticas(LocalDate fechaInicio, LocalDate fechaFin) {
        List<InstanciaReporte> instancias = instanciaRepo.findByFechaVencimientoCalculadaBetween(fechaInicio, fechaFin);
        return calcularEstadisticasDesdeInstancias(instancias);
    }

    public Map<String, Object> obtenerCumplimientoPorEntidad(LocalDate fechaInicio, LocalDate fechaFin) {
        List<InstanciaReporte> instancias = filtrarPorFechas(fechaInicio, fechaFin);

        Map<String, Map<String, Long>> cumplimientoPorEntidad = instancias.stream()
                .filter(i -> i.getReporte() != null && i.getReporte().getEntidad() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getReporte().getEntidad().getRazonSocial(),
                        Collectors.groupingBy(
                                this::clasificarEstado,
                                Collectors.counting())));

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("cumplimientoPorEntidad", cumplimientoPorEntidad);
        return resultado;
    }

    public Map<String, Object> obtenerCumplimientoPorResponsable(LocalDate fechaInicio, LocalDate fechaFin) {
        List<InstanciaReporte> instancias = filtrarPorFechas(fechaInicio, fechaFin);

        Map<String, Map<String, Long>> cumplimientoPorResponsable = instancias.stream()
                .filter(i -> i.getReporte() != null && i.getReporte().getResponsableElaboracion() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getReporte().getResponsableElaboracion().getNombreCompleto(),
                        Collectors.groupingBy(
                                this::clasificarEstado,
                                Collectors.counting())));

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("cumplimientoPorResponsable", cumplimientoPorResponsable);
        return resultado;
    }

    public Map<String, Object> obtenerTendenciaHistorica(int meses) {
        Map<String, Double> tendencia = new LinkedHashMap<>();
        LocalDate hoy = LocalDate.now();

        for (int i = meses - 1; i >= 0; i--) {
            YearMonth mes = YearMonth.from(hoy.minusMonths(i));
            LocalDate inicioMes = mes.atDay(1);
            LocalDate finMes = mes.atEndOfMonth();

            List<InstanciaReporte> instanciasMes = instanciaRepo.findByFechaVencimientoCalculadaBetween(inicioMes, finMes);

            long total = instanciasMes.size();
            long aTiempo = instanciasMes.stream()
                    .filter(inst -> inst.getDiasDesviacion() != null && inst.getDiasDesviacion() <= 0)
                    .filter(inst -> inst.getEstado() != null &&
                            inst.getEstado().getNombre().equalsIgnoreCase("Enviado"))
                    .count();

            double porcentaje = total > 0 ? (aTiempo * 100.0) / total : 0.0;
            tendencia.put(mes.format(DateTimeFormatter.ofPattern("yyyy-MM")), porcentaje);
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("tendencia", tendencia);
        return resultado;
    }

    public Map<String, Long> obtenerDistribucionEstados() {
        return instanciaRepo.findAll().stream()
                .filter(i -> i.getEstado() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getEstado().getNombre(),
                        Collectors.counting()));
    }

    public Map<String, Object> obtenerProximosAVencer(int dias) {
        LocalDate hoy = LocalDate.now();
        List<InstanciaReporte> instancias = instanciaRepo.findProximosAVencer(hoy, hoy.plusDays(dias));

        List<Map<String, Object>> reportesSimples = instancias.stream()
                .filter(i -> i.getReporte() != null)
                .map(i -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", i.getId());
                    map.put("reporteNombre", i.getReporte().getNombre());
                    map.put("entidadNombre", i.getReporte().getEntidad() != null 
                            ? i.getReporte().getEntidad().getRazonSocial() : "");
                    map.put("fechaVencimiento", i.getFechaVencimientoCalculada());
                    map.put("responsable", i.getReporte().getResponsableElaboracion() != null 
                            ? i.getReporte().getResponsableElaboracion().getNombreCompleto() : "Sin asignar");
                    long diasRestantes = i.getFechaVencimientoCalculada() != null 
                            ? ChronoUnit.DAYS.between(LocalDate.now(), i.getFechaVencimientoCalculada()) : 0;
                    map.put("diasRestantes", diasRestantes);
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("cantidad", reportesSimples.size());
        resultado.put("reportes", reportesSimples);

        return resultado;
    }

    public Map<String, Object> obtenerReportesVencidos() {
        LocalDate hoy = LocalDate.now();
        List<InstanciaReporte> instancias = instanciaRepo.findVencidos(hoy);

        List<Map<String, Object>> reportesSimples = instancias.stream()
                .filter(i -> i.getReporte() != null)
                .map(i -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", i.getId());
                    map.put("reporteNombre", i.getReporte().getNombre());
                    map.put("entidadNombre", i.getReporte().getEntidad() != null 
                            ? i.getReporte().getEntidad().getRazonSocial() : "");
                    map.put("fechaVencimiento", i.getFechaVencimientoCalculada());
                    map.put("responsable", i.getReporte().getResponsableElaboracion() != null 
                            ? i.getReporte().getResponsableElaboracion().getNombreCompleto() : "Sin asignar");
                    long diasVencido = i.getFechaVencimientoCalculada() != null 
                            ? ChronoUnit.DAYS.between(i.getFechaVencimientoCalculada(), LocalDate.now()) : 0;
                    map.put("diasVencido", diasVencido);
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("cantidad", reportesSimples.size());
        resultado.put("reportes", reportesSimples);

        return resultado;
    }

    public Map<String, Object> obtenerTopIncumplimientoEntidades(int top) {
        Map<String, Long> incumplimientos = instanciaRepo.findAll().stream()
                .filter(i -> i.getDiasDesviacion() != null && i.getDiasDesviacion() > 0)
                .filter(i -> i.getReporte() != null && i.getReporte().getEntidad() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getReporte().getEntidad().getRazonSocial(),
                        Collectors.counting()));

        List<Map.Entry<String, Long>> topList = incumplimientos.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(top)
                .toList();

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("top", topList);
        return resultado;
    }

    public Map<String, Object> obtenerTopIncumplimientoResponsables(int top) {
        Map<String, Long> incumplimientos = instanciaRepo.findAll().stream()
                .filter(i -> i.getDiasDesviacion() != null && i.getDiasDesviacion() > 0)
                .filter(i -> i.getReporte() != null && i.getReporte().getResponsableElaboracion() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getReporte().getResponsableElaboracion().getNombreCompleto(),
                        Collectors.counting()));

        List<Map.Entry<String, Long>> topList = incumplimientos.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(top)
                .toList();

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("top", topList);
        return resultado;
    }

    public Map<String, Object> obtenerResumenPorPeriodo(LocalDate fechaInicio, LocalDate fechaFin) {
        EstadisticasDTO stats = obtenerEstadisticas(fechaInicio, fechaFin);

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("periodo", Map.of(
                "inicio", fechaInicio,
                "fin", fechaFin));
        resumen.put("estadisticas", stats);

        return resumen;
    }

    // ================= RESPONSABLE =================

    public EstadisticasDTO obtenerDashboardResponsable(Integer responsableId, LocalDate inicio, LocalDate fin) {
        // Obtener SOLO las instancias donde el usuario es responsable de elaboración
        List<InstanciaReporte> instancias = instanciaRepo.findByResponsableAndFechaVencimiento(
                responsableId, inicio, fin);

        // Calcular estadísticas SOLO con esas instancias
        return calcularEstadisticasDesdeInstancias(instancias);
    }

    public Map<String, Object> obtenerProximosVencerResponsable(Integer responsableId, int dias) {
        LocalDate hoy = LocalDate.now();

        List<InstanciaReporte> instancias = instanciaRepo.findProximosPorResponsable(
                responsableId, hoy, hoy.plusDays(dias));

        return construirRespuestaProximos(instancias);
    }

    public Map<String, Object> obtenerVencidosResponsable(Integer responsableId) {
        List<InstanciaReporte> instancias = instanciaRepo.findVencidosPorResponsable(
                responsableId, LocalDate.now());

        return construirRespuestaVencidos(instancias);
    }

    // ================= SUPERVISOR =================

    public EstadisticasDTO obtenerDashboardSupervisor(Integer supervisorId, LocalDate inicio, LocalDate fin) {
        // Obtener SOLO las instancias donde el usuario es supervisor
        List<InstanciaReporte> instancias = instanciaRepo.findBySupervisorAndFechaVencimiento(
                supervisorId, inicio, fin);

        // Calcular estadísticas SOLO con esas instancias
        return calcularEstadisticasDesdeInstancias(instancias);
    }

    public Map<String, Object> obtenerProximosVencerSupervisor(Integer supervisorId, int dias) {
        LocalDate hoy = LocalDate.now();

        List<InstanciaReporte> instancias = instanciaRepo.findProximosPorSupervisor(
                supervisorId, hoy, hoy.plusDays(dias));

        return construirRespuestaProximos(instancias);
    }

    public Map<String, Object> obtenerVencidosSupervisor(Integer supervisorId) {
        List<InstanciaReporte> instancias = instanciaRepo.findVencidosPorSupervisor(
                supervisorId, LocalDate.now());

        return construirRespuestaVencidos(instancias);
    }

    // ================= MÉTODOS AUXILIARES =================

    /**
     * Calcula estadísticas a partir de una lista de instancias ya filtradas
     */
    private EstadisticasDTO calcularEstadisticasDesdeInstancias(List<InstanciaReporte> instancias) {
        EstadisticasDTO stats = new EstadisticasDTO();

        stats.setTotalObligaciones((long) instancias.size());

        long enviadosATiempo = instancias.stream()
                .filter(i -> i.getDiasDesviacion() != null && i.getDiasDesviacion() <= 0)
                .filter(i -> i.getEstado() != null &&
                        (i.getEstado().getNombre().toLowerCase().contains("enviado") ||
                                i.getEstado().getNombre().equalsIgnoreCase("Aprobado")))
                .count();

        long enviadosTarde = instancias.stream()
                .filter(i -> i.getDiasDesviacion() != null && i.getDiasDesviacion() > 0)
                .filter(i -> i.getEstado() != null &&
                        (i.getEstado().getNombre().toLowerCase().contains("enviado") ||
                                i.getEstado().getNombre().equalsIgnoreCase("Aprobado")))
                .count();

        long vencidos = instancias.stream()
                .filter(i -> i.getFechaVencimientoCalculada() != null &&
                        LocalDate.now().isAfter(i.getFechaVencimientoCalculada()))
                .filter(i -> i.getEstado() != null &&
                        !i.getEstado().getNombre().toLowerCase().contains("enviado") &&
                        !i.getEstado().getNombre().equalsIgnoreCase("Aprobado"))
                .count();

        long pendientes = instancias.stream()
                .filter(i -> i.getEstado() != null &&
                        (i.getEstado().getNombre().equalsIgnoreCase("Pendiente") ||
                                i.getEstado().getNombre().equalsIgnoreCase("En Proceso")))
                .count();

        stats.setTotalEnviadosATiempo(enviadosATiempo);
        stats.setTotalEnviadosTarde(enviadosTarde);
        stats.setTotalVencidos(vencidos);
        stats.setTotalPendientes(pendientes);

        if (stats.getTotalObligaciones() > 0) {
            stats.setPorcentajeCumplimientoATiempo(
                    (enviadosATiempo * 100.0) / stats.getTotalObligaciones());
        } else {
            stats.setPorcentajeCumplimientoATiempo(0.0);
        }

        double promedioRetraso = instancias.stream()
                .filter(i -> i.getDiasDesviacion() != null && i.getDiasDesviacion() > 0)
                .mapToInt(InstanciaReporte::getDiasDesviacion)
                .average()
                .orElse(0.0);
        stats.setDiasRetrasoPromedio(promedioRetraso);

        // Distribución por estado
        Map<String, Long> distribucion = instancias.stream()
                .filter(i -> i.getEstado() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getEstado().getNombre(),
                        Collectors.counting()));
        stats.setDistribucionEstados(distribucion);

        // Próximos a vencer en 7 días
        LocalDate hoy = LocalDate.now();
        long proximos7Dias = instancias.stream()
                .filter(i -> i.getFechaVencimientoCalculada() != null &&
                        i.getFechaVencimientoCalculada().isAfter(hoy) &&
                        i.getFechaVencimientoCalculada().isBefore(hoy.plusDays(8)))
                .filter(i -> i.getEstado() != null &&
                        !i.getEstado().getNombre().toLowerCase().contains("enviado") &&
                        !i.getEstado().getNombre().equalsIgnoreCase("Aprobado"))
                .count();
        stats.setReportesProximosVencer7Dias(proximos7Dias);

        long proximos3Dias = instancias.stream()
                .filter(i -> i.getFechaVencimientoCalculada() != null &&
                        i.getFechaVencimientoCalculada().isAfter(hoy) &&
                        i.getFechaVencimientoCalculada().isBefore(hoy.plusDays(4)))
                .filter(i -> i.getEstado() != null &&
                        !i.getEstado().getNombre().toLowerCase().contains("enviado") &&
                        !i.getEstado().getNombre().equalsIgnoreCase("Aprobado"))
                .count();
        stats.setReportesProximosVencer3Dias(proximos3Dias);

        // Alertas críticas (esto es global)
        stats.setAlertasCriticasActivas(0L);

        return stats;
    }

    private Map<String, Object> construirRespuestaProximos(List<InstanciaReporte> instancias) {
        List<Map<String, Object>> reportes = instancias.stream()
                .filter(i -> i.getReporte() != null)
                .map(i -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", i.getId());
                    map.put("reporteNombre", i.getReporte().getNombre());
                    map.put("entidadNombre", i.getReporte().getEntidad() != null 
                            ? i.getReporte().getEntidad().getRazonSocial() : "");
                    map.put("fechaVencimiento", i.getFechaVencimientoCalculada());
                    map.put("responsable", i.getReporte().getResponsableElaboracion() != null 
                            ? i.getReporte().getResponsableElaboracion().getNombreCompleto() : "Sin asignar");
                    long diasRestantes = i.getFechaVencimientoCalculada() != null 
                            ? ChronoUnit.DAYS.between(LocalDate.now(), i.getFechaVencimientoCalculada()) : 0;
                    map.put("diasRestantes", diasRestantes);
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("cantidad", reportes.size());
        result.put("reportes", reportes);
        return result;
    }

    private Map<String, Object> construirRespuestaVencidos(List<InstanciaReporte> instancias) {
        List<Map<String, Object>> reportes = instancias.stream()
                .filter(i -> i.getReporte() != null)
                .map(i -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", i.getId());
                    map.put("reporteNombre", i.getReporte().getNombre());
                    map.put("entidadNombre", i.getReporte().getEntidad() != null 
                            ? i.getReporte().getEntidad().getRazonSocial() : "");
                    map.put("fechaVencimiento", i.getFechaVencimientoCalculada());
                    map.put("responsable", i.getReporte().getResponsableElaboracion() != null 
                            ? i.getReporte().getResponsableElaboracion().getNombreCompleto() : "Sin asignar");
                    long diasVencido = i.getFechaVencimientoCalculada() != null 
                            ? ChronoUnit.DAYS.between(i.getFechaVencimientoCalculada(), LocalDate.now()) : 0;
                    map.put("diasVencido", diasVencido);
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("cantidad", reportes.size());
        result.put("reportes", reportes);
        return result;
    }

    private List<InstanciaReporte> filtrarPorFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null)
            fechaInicio = LocalDate.now().minusMonths(3);
        if (fechaFin == null)
            fechaFin = LocalDate.now();

        return instanciaRepo.findByFechaVencimientoCalculadaBetween(fechaInicio, fechaFin);
    }

    private String clasificarEstado(InstanciaReporte i) {
        if (i.getEstado() == null)
            return "Sin Estado";

        if (i.getEstado().getNombre().toLowerCase().contains("enviado") &&
                i.getDiasDesviacion() != null && i.getDiasDesviacion() <= 0) {
            return "A Tiempo";
        } else if (i.getEstado().getNombre().toLowerCase().contains("enviado") &&
                i.getDiasDesviacion() != null && i.getDiasDesviacion() > 0) {
            return "Tarde";
        } else if (i.getFechaVencimientoCalculada() != null &&
                LocalDate.now().isAfter(i.getFechaVencimientoCalculada())) {
            return "Vencido";
        } else {
            return "Pendiente";
        }
    }
    // ================= AUDITOR =================

    public Map<String, Object> obtenerDashboardAuditor(Integer anio, Integer mes, Integer trimestre) {
        LocalDate hoy = LocalDate.now();
        LocalDate fechaInicio;
        LocalDate fechaFin;

        // Determinar rango de fechas según filtros
        if (anio != null && mes != null) {
            // Filtro por mes específico
            fechaInicio = LocalDate.of(anio, mes, 1);
            fechaFin = fechaInicio.withDayOfMonth(fechaInicio.lengthOfMonth());
        } else if (anio != null && trimestre != null) {
            // Filtro por trimestre
            int mesInicio = (trimestre - 1) * 3 + 1;
            fechaInicio = LocalDate.of(anio, mesInicio, 1);
            fechaFin = fechaInicio.plusMonths(2).withDayOfMonth(fechaInicio.plusMonths(2).lengthOfMonth());
        } else if (anio != null) {
            // Filtro por año completo
            fechaInicio = LocalDate.of(anio, 1, 1);
            fechaFin = LocalDate.of(anio, 12, 31);
        } else {
            // Sin filtro: últimos 12 meses
            fechaInicio = hoy.minusMonths(12);
            fechaFin = hoy;
        }

        List<InstanciaReporte> instancias = instanciaRepo.findByFechaVencimientoCalculadaBetween(fechaInicio, fechaFin);

        // Estadísticas generales
        EstadisticasDTO stats = calcularEstadisticasDesdeInstancias(instancias);

        // Construir respuesta completa para auditor
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("estadisticas", stats);
        resultado.put("fechaInicio", fechaInicio);
        resultado.put("fechaFin", fechaFin);
        resultado.put("totalInstancias", instancias.size());

        // Cumplimiento por entidad
        Map<String, Map<String, Long>> porEntidad = instancias.stream()
                .filter(i -> i.getReporte() != null && i.getReporte().getEntidad() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getReporte().getEntidad().getRazonSocial(),
                        Collectors.groupingBy(this::clasificarEstado, Collectors.counting())));
        resultado.put("cumplimientoPorEntidad", porEntidad);

        // Cumplimiento por responsable
        Map<String, Map<String, Long>> porResponsable = instancias.stream()
                .filter(i -> i.getReporte() != null && i.getReporte().getResponsableElaboracion() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getReporte().getResponsableElaboracion().getNombreCompleto(),
                        Collectors.groupingBy(this::clasificarEstado, Collectors.counting())));
        resultado.put("cumplimientoPorResponsable", porResponsable);

        return resultado;
    }
}
