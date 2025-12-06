package com.example.demo.service;

import com.example.demo.entity.Reporte;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

@Service
public class FechaVencimientoCalculator {

    /**
     * Calcula la fecha de vencimiento para un reporte dado un período
     */
    public LocalDate calcularFechaVencimiento(Reporte reporte, String periodoReportado) {
        String frecuencia = reporte.getFrecuencia().getNombre().toUpperCase();
        LocalDate fechaBase;
        Integer diaVenc = reporte.getDiaVencimiento() != null ? reporte.getDiaVencimiento() : 15;

        switch (frecuencia) {
            case "MENSUAL":
                fechaBase = calcularVencimientoMensual(periodoReportado, diaVenc);
                break;
            case "BIMESTRAL":
                fechaBase = calcularVencimientoBimestral(periodoReportado, diaVenc);
                break;
            case "TRIMESTRAL":
                fechaBase = calcularVencimientoTrimestral(periodoReportado, diaVenc);
                break;
            case "SEMESTRAL":
                fechaBase = calcularVencimientoSemestral(periodoReportado, diaVenc);
                break;
            case "ANUAL":
                Integer mesVenc = reporte.getMesVencimiento() != null ? reporte.getMesVencimiento() : 3;
                fechaBase = calcularVencimientoAnual(periodoReportado, mesVenc, diaVenc);
                break;
            case "SEMANAL":
                fechaBase = calcularVencimientoSemanal(periodoReportado, diaVenc);
                break;
            case "DIARIA":
                fechaBase = LocalDate.parse(periodoReportado).plusDays(1);
                break;
            case "ÚNICA VEZ":
            case "UNICA VEZ":
            case "ESPECIFICA":
            case "ESPECÍFICA":
                // Para reportes de única vez, usar la fecha indicada en el período
                fechaBase = LocalDate.parse(periodoReportado);
                break;
            default:
                // Si no se reconoce la frecuencia, usar mensual como default
                fechaBase = calcularVencimientoMensual(periodoReportado, diaVenc);
        }

        // Agregar plazo adicional si existe
        if (reporte.getPlazoAdicionalDias() != null && reporte.getPlazoAdicionalDias() > 0) {
            fechaBase = fechaBase.plusDays(reporte.getPlazoAdicionalDias());
        }

        return fechaBase;
    }

    private LocalDate calcularVencimientoMensual(String periodoReportado, Integer diaVencimiento) {
        try {
            YearMonth yearMonth = YearMonth.parse(periodoReportado);
            YearMonth mesVencimiento = yearMonth.plusMonths(1);
            return mesVencimiento.atDay(Math.min(diaVencimiento, mesVencimiento.lengthOfMonth()));
        } catch (Exception e) {
            // Si no es formato YearMonth, intentar parsearlo de otra forma
            return LocalDate.now().plusMonths(1).withDayOfMonth(diaVencimiento);
        }
    }

    private LocalDate calcularVencimientoBimestral(String periodoReportado, Integer diaVencimiento) {
        try {
            String[] partes = periodoReportado.split("-B");
            int year = Integer.parseInt(partes[0]);
            int bimestre = Integer.parseInt(partes[1]);
            
            int mesInicio = (bimestre - 1) * 2 + 1;
            YearMonth ultimoMes = YearMonth.of(year, mesInicio + 1);
            
            return ultimoMes.plusMonths(1).atDay(Math.min(diaVencimiento, ultimoMes.plusMonths(1).lengthOfMonth()));
        } catch (Exception e) {
            return LocalDate.now().plusMonths(2).withDayOfMonth(diaVencimiento);
        }
    }

    private LocalDate calcularVencimientoTrimestral(String periodoReportado, Integer diaVencimiento) {
        try {
            String[] partes = periodoReportado.split("-Q");
            int year = Integer.parseInt(partes[0]);
            int trimestre = Integer.parseInt(partes[1]);
            
            int mesInicio = (trimestre - 1) * 3 + 1;
            YearMonth ultimoMes = YearMonth.of(year, mesInicio + 2);
            
            return ultimoMes.plusMonths(1).atDay(Math.min(diaVencimiento, ultimoMes.plusMonths(1).lengthOfMonth()));
        } catch (Exception e) {
            return LocalDate.now().plusMonths(3).withDayOfMonth(diaVencimiento);
        }
    }

    private LocalDate calcularVencimientoSemestral(String periodoReportado, Integer diaVencimiento) {
        try {
            String[] partes = periodoReportado.split("-S");
            int year = Integer.parseInt(partes[0]);
            int semestre = Integer.parseInt(partes[1]);
            
            int mesInicio = (semestre - 1) * 6 + 1;
            YearMonth ultimoMes = YearMonth.of(year, mesInicio + 5);
            
            return ultimoMes.plusMonths(1).atDay(Math.min(diaVencimiento, ultimoMes.plusMonths(1).lengthOfMonth()));
        } catch (Exception e) {
            return LocalDate.now().plusMonths(6).withDayOfMonth(diaVencimiento);
        }
    }

    private LocalDate calcularVencimientoAnual(String periodoReportado, Integer mesVencimiento, Integer diaVencimiento) {
        try {
            int year = Integer.parseInt(periodoReportado);
            YearMonth yearMonth = YearMonth.of(year + 1, mesVencimiento);
            return yearMonth.atDay(Math.min(diaVencimiento, yearMonth.lengthOfMonth()));
        } catch (Exception e) {
            return LocalDate.now().plusYears(1).withMonth(mesVencimiento).withDayOfMonth(diaVencimiento);
        }
    }

    private LocalDate calcularVencimientoSemanal(String periodoReportado, Integer diaVencimiento) {
        try {
            LocalDate fechaPeriodo = LocalDate.parse(periodoReportado);
            return fechaPeriodo.plusDays(7).with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.of(diaVencimiento)));
        } catch (Exception e) {
            return LocalDate.now().plusWeeks(1);
        }
    }

    /**
     * Calcula los días de desviación entre la fecha de envío y la fecha límite
     */
    public int calcularDiasDesviacion(LocalDate fechaEnvio, LocalDate fechaLimite) {
        if (fechaEnvio == null || fechaLimite == null) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(fechaLimite, fechaEnvio);
    }
}
