package com.example.demo.controller;

import com.example.demo.entity.EstadoCumplimiento;
import com.example.demo.service.EstadoCumplimientoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estados-cumplimiento")
@CrossOrigin
public class EstadoCumplimientoController {

    private final EstadoCumplimientoService service;

    public EstadoCumplimientoController(EstadoCumplimientoService service) {
        this.service = service;
    }

    @GetMapping
    public List<EstadoCumplimiento> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public EstadoCumplimiento obtener(@PathVariable Integer id) {
        return service.obtenerPorId(id);
    }
}
