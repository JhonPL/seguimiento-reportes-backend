package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para generar instancias de reportes automáticamente
 * basándose en la frecuencia y configuración de vencimiento del reporte.
 */
@Service
public class GeneradorInstanciasService {

    private final InstanciaReporteRepository instanciaRepo;
    private final EstadoCumplimientoRepository estadoRepo;
    private final FrecuenciaRepository frecuenciaRepo;
    private final AlertaRepository alertaRepo;

    public GeneradorInstanciasService(InstanciaReporteRepository instanciaRepo,
                                      EstadoCumplimientoRepository estadoRepo,
                                      FrecuenciaRepository frecuenciaRepo,
                                      AlertaRepository alertaRepo) {
        this.instanciaRepo = instanciaRepo;
        this.estadoRepo = estadoRepo;
        this.frecuenciaRepo = frecuenciaRepo;
        this.alertaRepo = alertaRepo;
    }

    /**
     * Genera instancias para un reporte desde una fecha inicio hasta una fecha fin.
     * Se utiliza al crear un nuevo reporte.
     */
    @Transactional
    public List<InstanciaReporte> generarInstanciasParaReporte(Reporte reporte, LocalDate fechaInicio, LocalDate fechaFin) {
        List<InstanciaReporte> instancias = new ArrayList<>();
        
        // Obtener estado "Pendiente"
        EstadoCumplimiento estadoPendiente = estadoRepo.findByNombre("Pendiente")
                .orElseThrow(() -> new RuntimeException("Estado 'Pendiente' no encontrado"));

        // Obtener nombre de frecuencia robustamente
        String nombreFreq = obtenerNombreFrecuencia(reporte);
        String frecuencia = nombreFreq.toUpperCase();
        int diaVencimiento = reporte.getDiaVencimiento() != null ? reporte.getDiaVencimiento() : 15;
        int mesVencimiento = reporte.getMesVencimiento() != null ? reporte.getMesVencimiento() : 1;

        List<LocalDate> fechasVencimiento = calcularFechasVencimiento(
                frecuencia, diaVencimiento, mesVencimiento, fechaInicio, fechaFin
        );

        for (LocalDate fechaVenc : fechasVencimiento) {
            // Verificar que no exista ya una instancia para este periodo
            String periodo = calcularPeriodoReportado(frecuencia, fechaVenc);
            
            boolean existe = instanciaRepo.findByReporte(reporte).stream()
                    .anyMatch(i -> i.getPeriodoReportado().equals(periodo));
            
            if (!existe) {
                InstanciaReporte instancia = new InstanciaReporte();
                instancia.setReporte(reporte);
                instancia.setPeriodoReportado(periodo);
                instancia.setFechaVencimientoCalculada(fechaVenc);
                instancia.setEstado(estadoPendiente);
                instancia.setDiasDesviacion(0);
                
                instancias.add(instanciaRepo.save(instancia));
            }
        }

        return instancias;
    }

    /**
     * Genera instancias para el año actual y el siguiente.
     * Útil al crear un reporte nuevo.
     */
    @Transactional
    public List<InstanciaReporte> generarInstanciasAnuales(Reporte reporte) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy.withDayOfMonth(1); // Primer día del mes actual
        LocalDate fin = hoy.plusYears(1).withMonth(12).withDayOfMonth(31); // Fin del año siguiente
        
        return generarInstanciasParaReporte(reporte, inicio, fin);
    }

    /**
     * Regenera las instancias de un reporte cuando su configuración cambió
     * (frecuencia, día/mes de vencimiento, fechas de vigencia, etc.).
     * 
     * COMPORTAMIENTO CORREGIDO:
     * - Elimina TODAS las instancias que NO han sido enviadas/aprobadas
     *   (incluyendo instancias VENCIDAS que aún no se enviaron)
     * - Genera nuevas instancias con la configuración actualizada
     * - Las instancias ya enviadas/aprobadas se PRESERVAN (son historial)
     */
    @Transactional
    public List<InstanciaReporte> regenerarInstanciasFuturas(Reporte reporte) {
        LocalDate hoy = LocalDate.now();

        // Buscar TODAS las instancias existentes para el reporte
        List<InstanciaReporte> todas = instanciaRepo.findByReporte(reporte);

        // Filtrar instancias que NO fueron enviadas/aprobadas
        // Esto incluye tanto FUTURAS como VENCIDAS que no se enviaron
        List<InstanciaReporte> aEliminar = new ArrayList<>();
        for (InstanciaReporte i : todas) {
            String estado = i.getEstado() != null ? i.getEstado().getNombre().toUpperCase() : "";
            
            // Solo preservamos las que ya fueron enviadas o aprobadas
            // Estados a preservar: "Enviado a tiempo", "Enviado tarde", "Aprobado"
            boolean enviada = estado.contains("ENVIADO") || estado.contains("APROBADO");
            
            if (!enviada) {
                // Eliminar si NO fue enviada (sin importar si está vencida o es futura)
                aEliminar.add(i);
                System.out.println("  → Marcada para eliminar: Periodo=" + i.getPeriodoReportado() 
                        + ", Vencimiento=" + i.getFechaVencimientoCalculada() 
                        + ", Estado=" + estado);
            }
        }

        if (!aEliminar.isEmpty()) {
            // Primero eliminar alertas asociadas a las instancias que se van a eliminar
            for (InstanciaReporte instancia : aEliminar) {
                List<Alerta> alertasInstancia = alertaRepo.findByInstancia(instancia);
                if (!alertasInstancia.isEmpty()) {
                    alertaRepo.deleteAll(alertasInstancia);
                    System.out.println("  → Eliminadas " + alertasInstancia.size() + " alertas de instancia " + instancia.getId());
                }
            }
            alertaRepo.flush();
            
            // Ahora eliminar las instancias
            instanciaRepo.deleteAll(aEliminar);
            instanciaRepo.flush(); // Forzar la eliminación inmediata
            System.out.println("♻️ Eliminadas " + aEliminar.size() + " instancias no enviadas para reporte " + reporte.getId());
        }

        // Determinar desde cuándo generar las nuevas instancias
        LocalDate inicio = hoy.withDayOfMonth(1);
        
        // Si la fecha de inicio de vigencia es posterior, usar esa
        if (reporte.getFechaInicioVigencia() != null && reporte.getFechaInicioVigencia().isAfter(inicio)) {
            inicio = reporte.getFechaInicioVigencia();
        }
        
        // Fecha fin: usar fechaFinVigencia o hasta el año siguiente
        LocalDate fin = reporte.getFechaFinVigencia() != null 
                ? reporte.getFechaFinVigencia() 
                : hoy.plusYears(1).withMonth(12).withDayOfMonth(31);

        // Solo generar si el reporte está activo
        List<InstanciaReporte> generadas = new ArrayList<>();
        if (reporte.isActivo()) {
            generadas = generarInstanciasParaReporte(reporte, inicio, fin);
            System.out.println("✓ Regeneradas " + generadas.size() + " instancias para reporte " + reporte.getId());
        } else {
            System.out.println("⚠️ Reporte " + reporte.getId() + " está inactivo. No se generaron nuevas instancias.");
        }
        
        return generadas;
    }

    /**
     * Obtiene el nombre de la frecuencia de forma robusta
     */
    private String obtenerNombreFrecuencia(Reporte reporte) {
        String nombreFreq = null;
        if (reporte.getFrecuencia() != null) {
            nombreFreq = reporte.getFrecuencia().getNombre();
            if (nombreFreq == null && reporte.getFrecuencia().getId() != null) {
                nombreFreq = frecuenciaRepo.findById(reporte.getFrecuencia().getId())
                        .map(Frecuencia::getNombre)
                        .orElse(null);
            }
        }
        return nombreFreq != null ? nombreFreq : "MENSUAL";
    }

    /**
     * Calcula las fechas de vencimiento según la frecuencia.
     */
    private List<LocalDate> calcularFechasVencimiento(String frecuencia, int dia, int mes, 
                                                       LocalDate inicio, LocalDate fin) {
        List<LocalDate> fechas = new ArrayList<>();

        switch (frecuencia) {
            case "MENSUAL":
                LocalDate fechaMensual = inicio.withDayOfMonth(Math.min(dia, inicio.lengthOfMonth()));
                if (fechaMensual.isBefore(inicio)) {
                    fechaMensual = fechaMensual.plusMonths(1);
                }
                while (!fechaMensual.isAfter(fin)) {
                    int diaAjustado = Math.min(dia, fechaMensual.lengthOfMonth());
                    fechas.add(fechaMensual.withDayOfMonth(diaAjustado));
                    fechaMensual = fechaMensual.plusMonths(1);
                }
                break;

            case "BIMESTRAL":
                LocalDate fechaBimestral = inicio.withDayOfMonth(Math.min(dia, inicio.lengthOfMonth()));
                if (fechaBimestral.isBefore(inicio)) {
                    fechaBimestral = fechaBimestral.plusMonths(2);
                }
                while (!fechaBimestral.isAfter(fin)) {
                    int diaAjustado = Math.min(dia, fechaBimestral.lengthOfMonth());
                    fechas.add(fechaBimestral.withDayOfMonth(diaAjustado));
                    fechaBimestral = fechaBimestral.plusMonths(2);
                }
                break;

            case "TRIMESTRAL":
                int[] iniciosTrimestre = {1, 4, 7, 10};
                for (int year = inicio.getYear(); year <= fin.getYear(); year++) {
                    for (int inicioTrim : iniciosTrimestre) {
                        int mesReal = inicioTrim + (mes - 1);
                        if (mesReal > 12) continue;
                        
                        YearMonth ym = YearMonth.of(year, mesReal);
                        int diaAjustado = Math.min(dia, ym.lengthOfMonth());
                        LocalDate fecha = LocalDate.of(year, mesReal, diaAjustado);
                        
                        if (!fecha.isBefore(inicio) && !fecha.isAfter(fin)) {
                            fechas.add(fecha);
                        }
                    }
                }
                break;

            case "CUATRIMESTRAL":
                int[] iniciosCuatrimestre = {1, 5, 9};
                for (int year = inicio.getYear(); year <= fin.getYear(); year++) {
                    for (int inicioCuat : iniciosCuatrimestre) {
                        int mesReal = inicioCuat + (mes - 1);
                        if (mesReal > 12) continue;
                        
                        YearMonth ym = YearMonth.of(year, mesReal);
                        int diaAjustado = Math.min(dia, ym.lengthOfMonth());
                        LocalDate fecha = LocalDate.of(year, mesReal, diaAjustado);
                        
                        if (!fecha.isBefore(inicio) && !fecha.isAfter(fin)) {
                            fechas.add(fecha);
                        }
                    }
                }
                break;

            case "SEMESTRAL":
                int[] iniciosSemestre = {1, 7};
                for (int year = inicio.getYear(); year <= fin.getYear(); year++) {
                    for (int inicioSem : iniciosSemestre) {
                        int mesReal = inicioSem + (mes - 1);
                        if (mesReal > 12) continue;
                        
                        YearMonth ym = YearMonth.of(year, mesReal);
                        int diaAjustado = Math.min(dia, ym.lengthOfMonth());
                        LocalDate fecha = LocalDate.of(year, mesReal, diaAjustado);
                        
                        if (!fecha.isBefore(inicio) && !fecha.isAfter(fin)) {
                            fechas.add(fecha);
                        }
                    }
                }
                break;

            case "ANUAL":
                for (int year = inicio.getYear(); year <= fin.getYear(); year++) {
                    YearMonth ym = YearMonth.of(year, mes);
                    int diaAjustado = Math.min(dia, ym.lengthOfMonth());
                    LocalDate fecha = LocalDate.of(year, mes, diaAjustado);
                    
                    if (!fecha.isBefore(inicio) && !fecha.isAfter(fin)) {
                        fechas.add(fecha);
                    }
                }
                break;

            case "ÚNICA VEZ":
            case "UNICA VEZ":
                YearMonth ymUnica = YearMonth.of(inicio.getYear(), mes);
                int diaUnico = Math.min(dia, ymUnica.lengthOfMonth());
                LocalDate fechaUnica = LocalDate.of(inicio.getYear(), mes, diaUnico);
                if (!fechaUnica.isBefore(inicio) && !fechaUnica.isAfter(fin)) {
                    fechas.add(fechaUnica);
                }
                break;

            case "ESPECÍFICA":
            case "ESPECIFICA":
                // Para frecuencia específica, usar la fecha de inicio de vigencia
                if (!inicio.isAfter(fin)) {
                    fechas.add(inicio);
                }
                break;

            default:
                // Por defecto, mensual
                LocalDate fechaDefault = inicio.withDayOfMonth(Math.min(dia, inicio.lengthOfMonth()));
                while (!fechaDefault.isAfter(fin)) {
                    fechas.add(fechaDefault);
                    fechaDefault = fechaDefault.plusMonths(1);
                }
        }

        return fechas;
    }

    /**
     * Calcula el periodo reportado basado en la frecuencia y fecha.
     */
    private String calcularPeriodoReportado(String frecuencia, LocalDate fecha) {
        int year = fecha.getYear();
        int month = fecha.getMonthValue();

        switch (frecuencia) {
            case "MENSUAL":
                return String.format("%d-%02d", year, month);
            
            case "BIMESTRAL":
                int bimestre = ((month - 1) / 2) + 1;
                return String.format("%d-B%d", year, bimestre);
            
            case "TRIMESTRAL":
                int trimestre = ((month - 1) / 3) + 1;
                return String.format("%d-T%d", year, trimestre);
            
            case "CUATRIMESTRAL":
                int cuatrimestre = ((month - 1) / 4) + 1;
                return String.format("%d-C%d", year, cuatrimestre);
            
            case "SEMESTRAL":
                int semestre = month <= 6 ? 1 : 2;
                return String.format("%d-S%d", year, semestre);
            
            case "ANUAL":
                return String.valueOf(year);
            
            case "ÚNICA VEZ":
            case "UNICA VEZ":
                return String.format("UNICO-%d", year);
            
            case "ESPECÍFICA":
            case "ESPECIFICA":
                return String.format("ESP-%d-%02d-%02d", year, month, fecha.getDayOfMonth());
            
            default:
                return String.format("%d-%02d", year, month);
        }
    }
}