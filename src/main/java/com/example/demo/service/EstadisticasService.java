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
                        (i.getEstado().getNombre().equalsIgnoreCase("Enviado") ||
                                i.getEstado().getNombre().equalsIgnoreCase("Aprobado")))
                .count();

        long enviadosTarde = instancias.stream()
                .filter(i -> i.getDiasDesviacion() != null && i.getDiasDesviacion() > 0)
                .filter(i -> i.getEstado() != null &&
                        (i.getEstado().getNombre().equalsIgnoreCase("Enviado") ||
                                i.getEstado().getNombre().equalsIgnoreCase("Aprobado")))
                .count();

        long vencidos = instancias.stream()
                .filter(i -> i.getFechaVencimientoCalculada() != null &&
                        LocalDate.now().isAfter(i.getFechaVencimientoCalculada()))
                .filter(i -> i.getEstado() != null &&
                        !i.getEstado().getNombre().equalsIgnoreCase("Enviado") &&
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
                        !i.getEstado().getNombre().equalsIgnoreCase("Enviado") &&
                        !i.getEstado().getNombre().equalsIgnoreCase("Aprobado"))
                .count();
        stats.setReportesProximosVencer7Dias(proximos7Dias);

        long proximos3Dias = instancias.stream()
                .filter(i -> i.getFechaVencimientoCalculada() != null &&
                        i.getFechaVencimientoCalculada().isAfter(hoy) &&
                        i.getFechaVencimientoCalculada().isBefore(hoy.plusDays(4)))
                .filter(i -> i.getEstado() != null &&
                        !i.getEstado().getNombre().equalsIgnoreCase("Enviado") &&
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

        String nombreEstado = i.getEstado().getNombre().toUpperCase();
        
        if (nombreEstado.contains("ENVIADO A TIEMPO")) {
            return "A Tiempo";
        } else if (nombreEstado.contains("ENVIADO TARDE")) {
            return "Tarde";
        } else if (nombreEstado.contains("VENCIDO") || 
                   (i.getFechaVencimientoCalculada() != null &&
                    LocalDate.now().isAfter(i.getFechaVencimientoCalculada()) &&
                    !nombreEstado.contains("ENVIADO"))) {
            return "Vencido";
        } else {
            return "Pendiente";
        }
    }

    // ================= DASHBOARD AUDITOR =================

    /**
     * Obtener estadísticas completas para el dashboard del auditor
     * Filtrable por año, mes y trimestre
     */
    public Map<String, Object> obtenerDashboardAuditor(Integer anio, Integer mes, Integer trimestre) {
        // Si no se especifica año, usar el actual
        int year = anio != null ? anio : LocalDate.now().getYear();
        
        // Obtener todas las instancias
        List<InstanciaReporte> todasInstancias = instanciaRepo.findAll();
        
        // Filtrar por año
        List<InstanciaReporte> instanciasFiltradas = todasInstancias.stream()
                .filter(i -> i.getFechaVencimientoCalculada() != null)
                .filter(i -> i.getFechaVencimientoCalculada().getYear() == year)
                .collect(Collectors.toList());
        
        // Filtrar por trimestre si se especifica
        if (trimestre != null && trimestre >= 1 && trimestre <= 4) {
            int mesInicio = (trimestre - 1) * 3 + 1;
            int mesFin = mesInicio + 2;
            instanciasFiltradas = instanciasFiltradas.stream()
                    .filter(i -> {
                        int mesInstancia = i.getFechaVencimientoCalculada().getMonthValue();
                        return mesInstancia >= mesInicio && mesInstancia <= mesFin;
                    })
                    .collect(Collectors.toList());
        }
        
        // Filtrar por mes si se especifica (tiene prioridad sobre trimestre)
        if (mes != null && mes >= 1 && mes <= 12) {
            instanciasFiltradas = instanciasFiltradas.stream()
                    .filter(i -> i.getFechaVencimientoCalculada().getMonthValue() == mes)
                    .collect(Collectors.toList());
        }
        
        // Calcular métricas
        Map<String, Object> resultado = new HashMap<>();
        
        int total = instanciasFiltradas.size();
        
        // Contar por estado
        long enviadosATiempo = instanciasFiltradas.stream()
                .filter(i -> i.getEstado() != null && 
                        i.getEstado().getNombre().toUpperCase().contains("ENVIADO A TIEMPO"))
                .count();
        
        long enviadosTarde = instanciasFiltradas.stream()
                .filter(i -> i.getEstado() != null && 
                        i.getEstado().getNombre().toUpperCase().contains("ENVIADO TARDE"))
                .count();
        
        long vencidos = instanciasFiltradas.stream()
                .filter(i -> {
                    if (i.getEstado() == null) return false;
                    String estado = i.getEstado().getNombre().toUpperCase();
                    // Es vencido si: estado es "Vencido" O (fecha pasada Y no enviado)
                    if (estado.contains("VENCIDO")) return true;
                    if (estado.contains("ENVIADO")) return false;
                    return i.getFechaVencimientoCalculada() != null && 
                           i.getFechaVencimientoCalculada().isBefore(LocalDate.now());
                })
                .count();
        
        long pendientes = instanciasFiltradas.stream()
                .filter(i -> {
                    if (i.getEstado() == null) return false;
                    String estado = i.getEstado().getNombre().toUpperCase();
                    if (estado.contains("ENVIADO") || estado.contains("VENCIDO")) return false;
                    // Es pendiente si fecha no ha pasado
                    return i.getFechaVencimientoCalculada() == null || 
                           !i.getFechaVencimientoCalculada().isBefore(LocalDate.now());
                })
                .count();
        
        // Porcentaje de cumplimiento
        double porcentajeCumplimiento = total > 0 ? (enviadosATiempo * 100.0) / total : 0;
        
        // Días de retraso promedio (de los enviados tarde)
        double diasRetrasoPromedio = instanciasFiltradas.stream()
                .filter(i -> i.getEstado() != null && 
                        i.getEstado().getNombre().toUpperCase().contains("ENVIADO TARDE"))
                .filter(i -> i.getDiasDesviacion() != null && i.getDiasDesviacion() > 0)
                .mapToInt(InstanciaReporte::getDiasDesviacion)
                .average()
                .orElse(0.0);
        
        // Métricas principales
        resultado.put("total", total);
        resultado.put("enviadosATiempo", enviadosATiempo);
        resultado.put("enviadosTarde", enviadosTarde);
        resultado.put("vencidos", vencidos);
        resultado.put("pendientes", pendientes);
        resultado.put("porcentajeCumplimiento", Math.round(porcentajeCumplimiento * 10) / 10.0);
        resultado.put("diasRetrasoPromedio", Math.round(diasRetrasoPromedio * 10) / 10.0);
        
        // Distribución para gráfico de torta
        List<Map<String, Object>> distribucion = new ArrayList<>();
        if (enviadosATiempo > 0) distribucion.add(Map.of("name", "A Tiempo", "value", enviadosATiempo, "color", "#10B981"));
        if (enviadosTarde > 0) distribucion.add(Map.of("name", "Tarde", "value", enviadosTarde, "color", "#F59E0B"));
        if (vencidos > 0) distribucion.add(Map.of("name", "Vencido", "value", vencidos, "color", "#EF4444"));
        if (pendientes > 0) distribucion.add(Map.of("name", "Pendiente", "value", pendientes, "color", "#6B7280"));
        resultado.put("distribucionEstados", distribucion);
        
        // Cumplimiento por entidad
        resultado.put("cumplimientoPorEntidad", calcularCumplimientoPorEntidad(instanciasFiltradas));
        
        // Cumplimiento por responsable
        resultado.put("cumplimientoPorResponsable", calcularCumplimientoPorResponsable(instanciasFiltradas));
        
        // Tendencia mensual del año
        resultado.put("tendenciaMensual", calcularTendenciaMensual(todasInstancias, year));
        
        // Años disponibles
        List<Integer> aniosDisponibles = todasInstancias.stream()
                .filter(i -> i.getFechaVencimientoCalculada() != null)
                .map(i -> i.getFechaVencimientoCalculada().getYear())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        resultado.put("aniosDisponibles", aniosDisponibles);
        
        return resultado;
    }

    private List<Map<String, Object>> calcularCumplimientoPorEntidad(List<InstanciaReporte> instancias) {
        Map<String, List<InstanciaReporte>> porEntidad = instancias.stream()
                .filter(i -> i.getReporte() != null && i.getReporte().getEntidad() != null)
                .collect(Collectors.groupingBy(i -> i.getReporte().getEntidad().getRazonSocial()));
        
        return porEntidad.entrySet().stream()
                .map(entry -> {
                    String entidad = entry.getKey();
                    List<InstanciaReporte> lista = entry.getValue();
                    int total = lista.size();
                    long aTiempo = lista.stream()
                            .filter(i -> i.getEstado() != null && 
                                    i.getEstado().getNombre().toUpperCase().contains("ENVIADO A TIEMPO"))
                            .count();
                    long vencidosEntidad = lista.stream()
                            .filter(i -> {
                                if (i.getEstado() == null) return false;
                                String estado = i.getEstado().getNombre().toUpperCase();
                                if (estado.contains("VENCIDO")) return true;
                                if (estado.contains("ENVIADO")) return false;
                                return i.getFechaVencimientoCalculada() != null && 
                                       i.getFechaVencimientoCalculada().isBefore(LocalDate.now());
                            })
                            .count();
                    int porcentaje = total > 0 ? (int) Math.round((aTiempo * 100.0) / total) : 0;
                    
                    Map<String, Object> map = new HashMap<>();
                    map.put("entidad", entidad);
                    map.put("total", total);
                    map.put("vencidos", vencidosEntidad);
                    map.put("porcentaje", porcentaje);
                    return map;
                })
                .sorted(Comparator.comparingInt(m -> (Integer) m.get("porcentaje")))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> calcularCumplimientoPorResponsable(List<InstanciaReporte> instancias) {
        Map<String, List<InstanciaReporte>> porResponsable = instancias.stream()
                .filter(i -> i.getReporte() != null && i.getReporte().getResponsableElaboracion() != null)
                .collect(Collectors.groupingBy(i -> i.getReporte().getResponsableElaboracion().getNombreCompleto()));
        
        return porResponsable.entrySet().stream()
                .map(entry -> {
                    String responsable = entry.getKey();
                    List<InstanciaReporte> lista = entry.getValue();
                    int total = lista.size();
                    long aTiempo = lista.stream()
                            .filter(i -> i.getEstado() != null && 
                                    i.getEstado().getNombre().toUpperCase().contains("ENVIADO A TIEMPO"))
                            .count();
                    long vencidosResp = lista.stream()
                            .filter(i -> {
                                if (i.getEstado() == null) return false;
                                String estado = i.getEstado().getNombre().toUpperCase();
                                if (estado.contains("VENCIDO")) return true;
                                if (estado.contains("ENVIADO")) return false;
                                return i.getFechaVencimientoCalculada() != null && 
                                       i.getFechaVencimientoCalculada().isBefore(LocalDate.now());
                            })
                            .count();
                    int porcentaje = total > 0 ? (int) Math.round((aTiempo * 100.0) / total) : 0;
                    
                    Map<String, Object> map = new HashMap<>();
                    map.put("responsable", responsable);
                    map.put("total", total);
                    map.put("vencidos", vencidosResp);
                    map.put("porcentaje", porcentaje);
                    return map;
                })
                .sorted(Comparator.comparingInt(m -> (Integer) m.get("porcentaje")))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> calcularTendenciaMensual(List<InstanciaReporte> todasInstancias, int year) {
        String[] meses = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
        List<Map<String, Object>> tendencia = new ArrayList<>();
        
        for (int m = 1; m <= 12; m++) {
            final int mes = m;
            List<InstanciaReporte> instanciasMes = todasInstancias.stream()
                    .filter(i -> i.getFechaVencimientoCalculada() != null)
                    .filter(i -> i.getFechaVencimientoCalculada().getYear() == year)
                    .filter(i -> i.getFechaVencimientoCalculada().getMonthValue() == mes)
                    .collect(Collectors.toList());
            
            int total = instanciasMes.size();
            long aTiempo = instanciasMes.stream()
                    .filter(i -> i.getEstado() != null && 
                            i.getEstado().getNombre().toUpperCase().contains("ENVIADO A TIEMPO"))
                    .count();
            int cumplimiento = total > 0 ? (int) Math.round((aTiempo * 100.0) / total) : 0;
            
            Map<String, Object> map = new HashMap<>();
            map.put("mes", meses[m - 1]);
            map.put("total", total);
            map.put("cumplimiento", cumplimiento);
            tendencia.add(map);
        }
        
        return tendencia;
    }
}