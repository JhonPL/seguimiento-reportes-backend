package com.example.demo.service;

import com.example.demo.entity.TipoAlerta;

import java.util.List;

public interface TipoAlertaService {
    List<TipoAlerta> listar();
    TipoAlerta obtenerPorId(Integer id);
}
