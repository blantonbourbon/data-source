package com.data.service.core.service;

import com.data.service.core.mapper.EntityMapper;
import com.data.service.core.search.GenericSpecification;
import com.data.service.core.search.MetricRequest;
import com.data.service.core.search.SearchCriteria;
import com.data.service.core.search.SearchRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Collections;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;

/**
 * Abstract generic service providing standard CRUD and metric operations.
 *
 * @param <M> the model/DTO type
 * @param <E> the JPA entity type
 */
public class GenericService<M, E> {

    private final JpaRepository<E, Long> repository;
    private final JpaSpecificationExecutor<E> specExecutor;
    private final EntityMapper<M, E> mapper;
    private final Class<M> modelClass;
    private final Class<E> entityClass;
    private final EntityManager entityManager;

    public GenericService(JpaRepository<E, Long> repository,
            JpaSpecificationExecutor<E> specExecutor,
            EntityMapper<M, E> mapper,
            Class<M> modelClass,
            Class<E> entityClass,
            EntityManager entityManager) {
        this.repository = repository;
        this.specExecutor = specExecutor;
        this.mapper = mapper;
        this.modelClass = modelClass;
        this.entityClass = entityClass;
        this.entityManager = entityManager;
    }

    public Class<M> getModelClass() {
        return modelClass;
    }

    public List<M> findAll() {
        return repository.findAll().stream()
                .map(mapper::toModel)
                .collect(Collectors.toList());
    }

    public M save(M model) {
        E entity = mapper.toEntity(model);
        E saved = repository.save(entity);
        return mapper.toModel(saved);
    }

    public M findById(Long id) {
        return repository.findById(id)
                .map(mapper::toModel)
                .orElse(null);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public List<M> query(SearchRequest request) {
        Specification<E> spec = buildSpecification(request.getConditions());
        return specExecutor.findAll(spec).stream()
                .map(mapper::toModel)
                .collect(Collectors.toList());
    }

    private Specification<E> buildSpecification(List<SearchCriteria> criteriaList) {
        Specification<E> spec = null;
        if (criteriaList != null) {
            for (SearchCriteria criteria : criteriaList) {
                Specification<E> nextSpec = new GenericSpecification<>(criteria);
                spec = (spec == null) ? nextSpec : spec.and(nextSpec);
            }
        }
        return spec;
    }

    public Object getMetric(MetricRequest request) {
        Specification<E> spec = buildSpecification(request.getFilters());

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<E> root = query.from(entityClass);

        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }

        String type = request.getMetricType() != null && !request.getMetricType().isEmpty() ? request.getMetricType().toUpperCase() : "COUNT";
        String fieldName = request.getField();
        List<String> groupByFields = request.getGroupBy() == null ? Collections.emptyList() : request.getGroupBy();

        List<Selection<?>> selections = new java.util.ArrayList<>();
        List<Expression<?>> groupExpressions = new java.util.ArrayList<>();

        for (String g : groupByFields) {
            Path<?> path = root.get(g);
            selections.add(path.alias(g));
            groupExpressions.add(path);
        }

        Expression<?> aggExpression;
        if (fieldName != null && !fieldName.isEmpty()) {
            Path<Number> path = root.get(fieldName);
            switch (type) {
                case "SUM": aggExpression = cb.sum(path); break;
                case "AVG": aggExpression = cb.avg(path); break;
                case "MAX": aggExpression = cb.max(path); break;
                case "MIN": aggExpression = cb.min(path); break;
                default: aggExpression = cb.count(path); break;
            }
        } else {
            aggExpression = cb.count(root);
        }
        selections.add(aggExpression.alias("metricValue"));

        query.multiselect(selections);
        if (!groupExpressions.isEmpty()) {
            query.groupBy(groupExpressions);
        }

        List<Tuple> resultList = entityManager.createQuery(query).getResultList();

        if (groupByFields.isEmpty()) {
            if (resultList.isEmpty()) return 0.0;
            return resultList.get(0).get("metricValue");
        }

        return resultList.stream().map(tuple -> {
            Map<String, Object> map = new LinkedHashMap<>();
            for (String g : groupByFields) {
                map.put(g, tuple.get(g));
            }
            map.put(type.toLowerCase(), tuple.get("metricValue"));
            return map;
        }).collect(Collectors.toList());
    }
}
