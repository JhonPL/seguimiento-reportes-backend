package com.example.demo.controller;

import com.example.demo.entity.Reporte;
import com.example.demo.service.ReporteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@CrossOrigin
public class ReporteController {

    private final ReporteService service;

    public ReporteController(ReporteService service) {
        this.service = service;
    }

    @GetMapping
    public List<Reporte> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Reporte obtener(@PathVariable String id) {
        return service.obtenerPorId(id);
    }

    @PostMapping
    public Reporte crear(@RequestBody Reporte reporte) {
        return service.crear(reporte);
    }

    @PutMapping("/{id}")
    public Reporte actualizar(@PathVariable String id, @RequestBody Reporte reporte) {
        return service.actualizar(id, reporte);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable String id) {
        service.eliminar(id);
    }

    @GetMapping("/entidad/{idEntidad}")
    public List<Reporte> porEntidad(@PathVariable Integer idEntidad) {
        return service.listarPorEntidad(idEntidad);
    }

    @GetMapping("/frecuencia/{idFrecuencia}")
    public List<Reporte> porFrecuencia(@PathVariable Integer idFrecuencia) {
        return service.listarPorFrecuencia(idFrecuencia);
    }
}
