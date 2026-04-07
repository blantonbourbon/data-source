package com.data.service.core.controller;

import com.data.service.core.search.MetricRequest;
import com.data.service.core.search.SearchRequest;
import com.data.service.core.service.GenericService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenericControllerTest {

    @Mock
    private GenericService<TestModel, TestEntity> service;

    private TestController testController;

    @BeforeEach
    void setUp() {
        testController = new TestController(service);
    }

    @Test
    void testGetAll() {
        TestModel model = new TestModel(1L, "value");
        when(service.findAll()).thenReturn(List.of(model));

        List<TestModel> result = testController.getAll();

        assertEquals(1, result.size());
        assertEquals(model, result.get(0));
        verify(service, times(1)).findAll();
    }

    @Test
    void testCreate() {
        TestModel model = new TestModel(1L, "value");
        when(service.save(model)).thenReturn(model);

        TestModel result = testController.create(model);

        assertEquals(model, result);
        verify(service, times(1)).save(model);
    }

    @Test
    void testGetByIdFound() {
        TestModel model = new TestModel(1L, "value");
        when(service.findById(1L)).thenReturn(model);

        ResponseEntity<TestModel> response = testController.getById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(model, response.getBody());
        verify(service, times(1)).findById(1L);
    }

    @Test
    void testGetByIdNotFound() {
        when(service.findById(1L)).thenReturn(null);

        ResponseEntity<TestModel> response = testController.getById(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(service, times(1)).findById(1L);
    }

    @Test
    void testDelete() {
        ResponseEntity<Void> response = testController.delete(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(service, times(1)).deleteById(1L);
    }

    @Test
    void testGetMetric() {
        MetricRequest request = new MetricRequest();
        when(service.getMetric(request)).thenReturn(10.5);

        ResponseEntity<Object> response = testController.getMetric(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(10.5, response.getBody());
        verify(service, times(1)).getMetric(request);
    }

    @Test
    void testQuery() {
        SearchRequest request = new SearchRequest();
        TestModel model = new TestModel(2L, "query-result");
        when(service.query(request)).thenReturn(List.of(model));

        ResponseEntity<List<TestModel>> response = testController.query(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(model), response.getBody());
        verify(service, times(1)).query(request);
    }

    static class TestModel {
        Long id;
        String name;

        TestModel(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    static class TestEntity {
        Long id;
        String name;
    }

    static class TestController extends GenericController<TestModel, TestEntity> {
        public TestController(GenericService<TestModel, TestEntity> service) {
            super(service);
        }
    }
}
