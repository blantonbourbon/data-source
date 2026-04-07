package com.data.service.core.search;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;

public class GenericSpecification<T> implements Specification<T> {

    private SearchCriteria criteria;

    public GenericSpecification(SearchCriteria criteria) {
        this.criteria = criteria;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

        String key = criteria.getKey();
        Object value = criteria.getValue();
        SearchOperation op = criteria.getOperation();

        // Handle LocalDate parsing if needed
        if (root.get(key).getJavaType() == LocalDate.class && value instanceof String) {
            value = LocalDate.parse((String) value);
        }

        // Handle Global Keyword Search
        if ("_keyword_".equals(key)) {
            String keyword = "%" + value + "%";
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();
            root.getModel().getAttributes().forEach(attr -> {
                if (attr.getJavaType() == String.class) {
                    predicates.add(builder.like(root.get(attr.getName()), keyword));
                }
            });
            return builder.or(predicates.toArray(new Predicate[0]));
        }

        switch (op) {
            case EQUALITY:
                return builder.equal(root.get(key), value);
            case NEGATION:
                return builder.notEqual(root.get(key), value);
            case GREATER_THAN:
                if (value instanceof LocalDate) {
                    return builder.greaterThan(root.get(key), (LocalDate) value);
                }
                if (value instanceof Number) {
                    return builder.gt(root.get(key).as(Double.class), ((Number) value).doubleValue());
                }
                return builder.greaterThan(root.get(key), value.toString());
            case GREATER_THAN_EQUAL:
                if (value instanceof LocalDate) {
                    return builder.greaterThanOrEqualTo(root.get(key), (LocalDate) value);
                }
                if (value instanceof Number) {
                    return builder.ge(root.get(key).as(Double.class), ((Number) value).doubleValue());
                }
                return builder.greaterThanOrEqualTo(root.get(key), value.toString());
            case LESS_THAN:
                if (value instanceof LocalDate) {
                    return builder.lessThan(root.get(key), (LocalDate) value);
                }
                if (value instanceof Number) {
                    return builder.lt(root.get(key).as(Double.class), ((Number) value).doubleValue());
                }
                return builder.lessThan(root.get(key), value.toString());
            case LESS_THAN_EQUAL:
                if (value instanceof LocalDate) {
                    return builder.lessThanOrEqualTo(root.get(key), (LocalDate) value);
                }
                if (value instanceof Number) {
                    return builder.le(root.get(key).as(Double.class), ((Number) value).doubleValue());
                }
                return builder.lessThanOrEqualTo(root.get(key), value.toString());
            case LIKE:
                return builder.like(root.get(key), "%" + value + "%");
            case STARTS_WITH:
                return builder.like(root.get(key), value + "%");
            case ENDS_WITH:
                return builder.like(root.get(key), "%" + value);
            case CONTAINS:
                return builder.like(root.get(key), "%" + value + "%");
            case IN:
                if (value instanceof java.util.Collection) {
                    return root.get(key).in((java.util.Collection<?>) value);
                } else if (value instanceof String) {
                    java.util.List<String> list = java.util.Arrays.stream(((String) value).split(","))
                            .map(String::trim)
                            .collect(java.util.stream.Collectors.toList());
                    return root.get(key).in(list);
                }
                return builder.equal(root.get(key), value);
            default:
                return null;
        }
    }
}
