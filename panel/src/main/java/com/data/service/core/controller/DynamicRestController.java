package com.data.service.core.controller;

import com.data.service.core.search.MetricRequest;
import com.data.service.core.search.SearchRequest;
import com.data.service.core.service.GenericService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Single dynamic REST controller that handles all entity CRUD operations.
 * Routes /api/{entity}/** requests to the correct GenericService via
 * EntityRegistry.
 */
@RestController
@RequestMapping("/api/{entity}")
@RequiredArgsConstructor
@SuppressWarnings({ "unchecked", "rawtypes" })
public class DynamicRestController {

    private final EntityRegistry registry;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<?> getAll(@PathVariable String entity) {
        GenericService service = getServiceOrThrow(entity);
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String entity, @PathVariable Long id) {
        GenericService service = getServiceOrThrow(entity);
        Object result = service.findById(id);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String entity, @RequestBody Map<String, Object> body) {
        GenericService service = getServiceOrThrow(entity);
        Object model = objectMapper.convertValue(body, service.getModelClass());
        Object saved = service.save(model);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String entity, @PathVariable Long id) {
        getServiceOrThrow(entity);
        registry.getService(entity).deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/metric")
    public ResponseEntity<?> getMetric(@PathVariable String entity, @RequestBody MetricRequest request) {
        GenericService service = getServiceOrThrow(entity);
        return ResponseEntity.ok(service.getMetric(request));
    }

    @PostMapping("/query")
    public ResponseEntity<?> query(@PathVariable String entity, @RequestBody SearchRequest request) {
        GenericService service = getServiceOrThrow(entity);
        return ResponseEntity.ok(service.query(request));
    }


    private GenericService getServiceOrThrow(String entity) {
        if (!registry.hasEntity(entity)) {
            throw new EntityNotFoundException("Unknown entity: " + entity);
        }
        return registry.getService(entity);
    }

    @ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
    private static class EntityNotFoundException extends RuntimeException {
        public EntityNotFoundException(String message) {
            super(message);
        }
    }
}
