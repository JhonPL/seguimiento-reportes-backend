package com.example.demo.controller;

import com.example.demo.entity.Rol;
import com.example.demo.service.RolService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin
public class RolController {

    private final RolService service;

    public RolController(RolService service) {
        this.service = service;
    }

    @GetMapping
    public List<Rol> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Rol obtener(@PathVariable Integer id) {
        return service.obtenerPorId(id);
    }

    @PostMapping
    public Rol crear(@RequestBody Rol rol) {
        return service.crear(rol);
    }

    @PutMapping("/{id}")
    public Rol actualizar(@PathVariable Integer id, @RequestBody Rol rol) {
        return service.actualizar(id, rol);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        service.eliminar(id);
    }
}
