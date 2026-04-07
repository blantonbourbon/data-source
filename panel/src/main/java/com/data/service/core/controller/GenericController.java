package com.data.service.core.controller;

import com.data.service.core.search.MetricRequest;
import com.data.service.core.search.SearchRequest;
import com.data.service.core.service.GenericService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Abstract generic REST controller providing standard CRUD and metric
 * endpoints.
 * Subclasses only need to add @RestController, @RequestMapping, and pass their
 * service.
 *
 * @param <M> the model/DTO type
 * @param <E> the JPA entity type
 */
public abstract class GenericController<M, E> {

    private final GenericService<M, E> service;

    protected GenericController(GenericService<M, E> service) {
        this.service = service;
    }

    @GetMapping
    public List<M> getAll() {
        return service.findAll();
    }

    @PostMapping
    public M create(@RequestBody M entity) {
        return service.save(entity);
    }

    @GetMapping("/{id}")
    public ResponseEntity<M> getById(@PathVariable Long id) {
        M entity = service.findById(id);
        return entity != null ? ResponseEntity.ok(entity) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/metric")
    public ResponseEntity<Object> getMetric(@RequestBody MetricRequest request) {
        return ResponseEntity.ok(service.getMetric(request));
    }

    @PostMapping("/query")
    public ResponseEntity<List<M>> query(@RequestBody SearchRequest request) {
        return ResponseEntity.ok(service.query(request));
    }
}
