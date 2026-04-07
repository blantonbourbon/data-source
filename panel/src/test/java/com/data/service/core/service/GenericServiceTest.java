package com.data.service.core.service;

import com.data.service.core.mapper.EntityMapper;
import com.data.service.core.search.MetricRequest;
import com.data.service.core.search.SearchRequest;
import com.data.service.core.search.SearchCriteria;
import com.data.service.core.search.SearchOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenericServiceTest {

    @Mock
    private JpaRepository<TestEntity, Long> repository;

    @Mock
    private JpaSpecificationExecutor<TestEntity> specExecutor;

    @Mock
    private EntityMapper<TestModel, TestEntity> mapper;

    @Mock
    private EntityManager entityManager;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private CriteriaQuery<Tuple> criteriaQuery;

    @Mock
    private Root<TestEntity> root;

    @Mock
    private TypedQuery<Tuple> typedQuery;

    @Mock
    private Predicate predicate;

    private TestService testService;

    @BeforeEach
    void setUp() {
        testService = new TestService(repository, specExecutor, mapper, entityManager);
    }

    @Test
    void testFindAll() {
        TestEntity entity = new TestEntity(1L, "value", 100.0);
        TestModel model = new TestModel(1L, "value", 100.0);

        when(repository.findAll()).thenReturn(List.of(entity));
        when(mapper.toModel(entity)).thenReturn(model);

        List<TestModel> result = testService.findAll();

        assertEquals(1, result.size());
        assertEquals(model, result.get(0));
        verify(repository, times(1)).findAll();
    }

    @Test
    void testFindById() {
        TestEntity entity = new TestEntity(1L, "value", 100.0);
        TestModel model = new TestModel(1L, "value", 100.0);

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toModel(entity)).thenReturn(model);

        TestModel result = testService.findById(1L);

        assertEquals(model, result);
        verify(repository, times(1)).findById(1L);
    }

    @Test
    void testSave() {
        TestEntity entity = new TestEntity(1L, "value", 100.0);
        TestModel model = new TestModel(1L, "value", 100.0);

        when(mapper.toEntity(model)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toModel(entity)).thenReturn(model);

        TestModel result = testService.save(model);

        assertEquals(model, result);
        verify(repository, times(1)).save(entity);
    }

    @Test
    void testDeleteById() {
        testService.deleteById(1L);
        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetMetricCount() {
        MetricRequest request = new MetricRequest();
        request.setMetricType("COUNT");
        Expression<Long> countExpression = mock(Expression.class);
        Tuple tuple = mock(Tuple.class);

        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createTupleQuery()).thenReturn(criteriaQuery);
        when(criteriaQuery.from(TestEntity.class)).thenReturn(root);
        when(criteriaBuilder.count(root)).thenReturn(countExpression);
        when(entityManager.createQuery(criteriaQuery)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(List.of(tuple));
        when(tuple.get("metricValue")).thenReturn(5L);

        Object result = testService.getMetric(request);

        assertEquals(5L, result);
        verify(entityManager, times(1)).createQuery(criteriaQuery);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetMetricSum() {
        MetricRequest request = new MetricRequest();
        request.setMetricType("SUM");
        request.setField("doubleField");
        Expression<Number> sumExpression = mock(Expression.class);
        @SuppressWarnings("rawtypes")
        Path numberPath = mock(Path.class);
        Tuple tuple = mock(Tuple.class);

        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createTupleQuery()).thenReturn(criteriaQuery);
        when(criteriaQuery.from(TestEntity.class)).thenReturn(root);
        when(root.get("doubleField")).thenReturn(numberPath);
        when(criteriaBuilder.sum(numberPath)).thenReturn(sumExpression);
        when(entityManager.createQuery(criteriaQuery)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(List.of(tuple));
        when(tuple.get("metricValue")).thenReturn(75.5);

        Object result = testService.getMetric(request);

        assertEquals(75.5, result);
        verify(entityManager, times(1)).createQuery(criteriaQuery);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetMetricGroupBy() {
        MetricRequest request = new MetricRequest();
        request.setMetricType("SUM");
        request.setField("doubleField");
        request.setGroupBy(List.of("stringField"));
        @SuppressWarnings("rawtypes")
        Path groupPath = mock(Path.class);
        @SuppressWarnings("rawtypes")
        Path numberPath = mock(Path.class);
        Expression<Number> sumExpression = mock(Expression.class);
        Tuple tuple1 = mock(Tuple.class);
        Tuple tuple2 = mock(Tuple.class);

        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createTupleQuery()).thenReturn(criteriaQuery);
        when(criteriaQuery.from(TestEntity.class)).thenReturn(root);
        when(root.get("stringField")).thenReturn(groupPath);
        when(root.get("doubleField")).thenReturn(numberPath);
        when(criteriaBuilder.sum(numberPath)).thenReturn(sumExpression);
        when(entityManager.createQuery(criteriaQuery)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(List.of(tuple1, tuple2));
        when(tuple1.get("stringField")).thenReturn("group1");
        when(tuple1.get("metricValue")).thenReturn(75.0);
        when(tuple2.get("stringField")).thenReturn("group2");
        when(tuple2.get("metricValue")).thenReturn(10.0);

        Object result = testService.getMetric(request);

        assertTrue(result instanceof List);
        List<Map<String, Object>> list = (List<Map<String, Object>>) result;
        assertEquals(2, list.size());

        Map<String, Object> g1 = list.stream().filter(m -> "group1".equals(m.get("stringField"))).findFirst().get();
        assertEquals(75.0, g1.get("sum"));

        Map<String, Object> g2 = list.stream().filter(m -> "group2".equals(m.get("stringField"))).findFirst().get();
        assertEquals(10.0, g2.get("sum"));
    }

    @Test
    void testQuery() {
        SearchRequest request = new SearchRequest();
        request.setConditions(Collections.singletonList(new SearchCriteria("stringField", SearchOperation.EQUALITY, "value")));

        TestEntity entity = new TestEntity(1L, "value", 100.0);
        TestModel model = new TestModel(1L, "value", 100.0);

        when(specExecutor.findAll(any(Specification.class))).thenReturn(List.of(entity));
        when(mapper.toModel(entity)).thenReturn(model);

        List<TestModel> result = testService.query(request);

        assertEquals(List.of(model), result);
        verify(specExecutor, times(1)).findAll(any(Specification.class));
    }

    static class TestModel {
        Long id;
        String stringField;
        Double doubleField;

        TestModel(Long id, String stringField, Double doubleField) {
            this.id = id;
            this.stringField = stringField;
            this.doubleField = doubleField;
        }
    }

    static class TestEntity {
        Long id;
        String stringField;
        Double doubleField;

        TestEntity(Long id, String stringField, Double doubleField) {
            this.id = id;
            this.stringField = stringField;
            this.doubleField = doubleField;
        }
    }

    static class TestService extends GenericService<TestModel, TestEntity> {
        public TestService(JpaRepository<TestEntity, Long> repository,
                JpaSpecificationExecutor<TestEntity> specExecutor,
                EntityMapper<TestModel, TestEntity> mapper,
                EntityManager entityManager) {
            super(repository, specExecutor, mapper, TestModel.class, TestEntity.class, entityManager);
        }
    }
}
