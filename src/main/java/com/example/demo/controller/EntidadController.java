package com.example.demo.controller;

import com.example.demo.entity.Entidad;
import com.example.demo.service.EntidadService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entidades")
@CrossOrigin
public class EntidadController {

    private final EntidadService service;

    public EntidadController(EntidadService service) {
        this.service = service;
    }

    @GetMapping
    public List<Entidad> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Entidad obtener(@PathVariable Integer id) {
        return service.obtenerPorId(id);
    }

    @PostMapping
    public Entidad crear(@RequestBody Entidad entidad) {
        return service.crear(entidad);
    }

    @PutMapping("/{id}")
    public Entidad actualizar(@PathVariable Integer id, @RequestBody Entidad entidad) {
        return service.actualizar(id, entidad);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        service.eliminar(id);
    }
}
