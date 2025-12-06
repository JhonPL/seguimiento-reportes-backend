package com.example.demo.controller;

import com.example.demo.entity.TipoAlerta;
import com.example.demo.service.TipoAlertaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-alerta")
@CrossOrigin
public class TipoAlertaController {

    private final TipoAlertaService service;

    public TipoAlertaController(TipoAlertaService service) {
        this.service = service;
    }

    @GetMapping
    public List<TipoAlerta> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public TipoAlerta obtener(@PathVariable Integer id) {
        return service.obtenerPorId(id);
    }
}
