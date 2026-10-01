package com.osx23.avi.api.controller;

import com.osx23.avi.api.model.RegistroRequest;
import com.osx23.avi.api.model.RegistroResponse;
import com.osx23.avi.api.service.SupabaseRegistroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/registros")
public class RegistroController {

    private final SupabaseRegistroService service;

    public RegistroController(SupabaseRegistroService service) {
        this.service = service;
    }

    @GetMapping
    public List<RegistroResponse> listar() {
        return service.findRecent();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroResponse registrar(@Valid @RequestBody RegistroRequest request) {
        return service.create(request);
    }
}
