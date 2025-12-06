package com.example.demo.controller;

import com.example.demo.entity.Frecuencia;
import com.example.demo.service.FrecuenciaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/frecuencias")
@CrossOrigin
public class FrecuenciaController {

    private final FrecuenciaService service;

    public FrecuenciaController(FrecuenciaService service) {
        this.service = service;
    }

    @GetMapping
    public List<Frecuencia> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Frecuencia obtener(@PathVariable Integer id) {
        return service.obtenerPorId(id);
    }
}
