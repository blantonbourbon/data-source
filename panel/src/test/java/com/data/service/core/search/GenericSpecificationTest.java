package com.data.service.core.search;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({ "rawtypes", "unchecked" })
class GenericSpecificationTest {

    @Mock
    private Root<Object> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder builder;

    @Mock
    private Path path;

    @Mock
    private EntityType<Object> model;

    @Mock
    private Attribute<Object, String> stringAttribute;

    @Mock
    private Attribute<Object, Integer> intAttribute;

    @BeforeEach
    void setUp() {
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(path.getJavaType()).thenReturn((Class) String.class);
    }

    @Test
    void testEquality() {
        SearchCriteria criteria = new SearchCriteria("field", SearchOperation.EQUALITY, "value");
        GenericSpecification<Object> spec = new GenericSpecification<>(criteria);

        spec.toPredicate(root, query, builder);
        verify(builder).equal(path, "value");
    }

    @Test
    void testNegation() {
        SearchCriteria criteria = new SearchCriteria("field", SearchOperation.NEGATION, "value");
        GenericSpecification<Object> spec = new GenericSpecification<>(criteria);

        spec.toPredicate(root, query, builder);
        verify(builder).notEqual(path, "value");
    }

    @Test
    void testLike() {
        SearchCriteria criteria = new SearchCriteria("field", SearchOperation.LIKE, "val");
        GenericSpecification<Object> spec = new GenericSpecification<>(criteria);

        spec.toPredicate(root, query, builder);
        verify(builder).like(any(Path.class), eq("%val%"));
    }

    @Test
    void testStartsWith() {
        SearchCriteria criteria = new SearchCriteria("field", SearchOperation.STARTS_WITH, "val");
        GenericSpecification<Object> spec = new GenericSpecification<>(criteria);

        spec.toPredicate(root, query, builder);
        verify(builder).like(any(Path.class), eq("val%"));
    }

    @Test
    void testEndsWith() {
        SearchCriteria criteria = new SearchCriteria("field", SearchOperation.ENDS_WITH, "val");
        GenericSpecification<Object> spec = new GenericSpecification<>(criteria);

        spec.toPredicate(root, query, builder);
        verify(builder).like(any(Path.class), eq("%val"));
    }

    @Test
    void testGreaterThanNumber() {
        SearchCriteria criteria = new SearchCriteria("field", SearchOperation.GREATER_THAN, 10.5);
        GenericSpecification<Object> spec = new GenericSpecification<>(criteria);

        lenient().when(path.as(Double.class)).thenReturn(path);
        spec.toPredicate(root, query, builder);
        verify(builder).gt(any(Path.class), eq(10.5));
    }

    @Test
    void testLessThanNumber() {
        SearchCriteria criteria = new SearchCriteria("field", SearchOperation.LESS_THAN, 10.5);
        GenericSpecification<Object> spec = new GenericSpecification<>(criteria);

        lenient().when(path.as(Double.class)).thenReturn(path);
        spec.toPredicate(root, query, builder);
        verify(builder).lt(any(Path.class), eq(10.5));
    }

    @Test
    void testGreaterThanLocalDate() {
        LocalDate date = LocalDate.of(2023, 1, 1);
        SearchCriteria criteria = new SearchCriteria("field", SearchOperation.GREATER_THAN, date);
        GenericSpecification<Object> spec = new GenericSpecification<>(criteria);

        spec.toPredicate(root, query, builder);
        verify(builder).greaterThan(any(Path.class), eq(date));
    }

    @Test
    void testLocalDateParsing() {
        SearchCriteria criteria = new SearchCriteria("field", SearchOperation.EQUALITY, "2023-01-01");
        GenericSpecification<Object> spec = new GenericSpecification<>(criteria);

        lenient().when(path.getJavaType()).thenReturn((Class) LocalDate.class);

        spec.toPredicate(root, query, builder);
        verify(builder).equal(any(Path.class), eq(LocalDate.of(2023, 1, 1)));
    }

    @Test
    void testKeywordSearch() {
        SearchCriteria criteria = new SearchCriteria("_keyword_", SearchOperation.EQUALITY, "test");
        GenericSpecification<Object> spec = new GenericSpecification<>(criteria);

        lenient().when(root.getModel()).thenReturn(model);
        lenient().when(model.getAttributes()).thenReturn((Set) Set.of(stringAttribute, intAttribute));

        lenient().when(stringAttribute.getJavaType()).thenReturn((Class) String.class);
        lenient().when(stringAttribute.getName()).thenReturn("stringField");
        
        lenient().when(intAttribute.getJavaType()).thenReturn((Class) Integer.class);
        
        Path stringPath = mock(Path.class);
        lenient().when(root.get("stringField")).thenReturn(stringPath);
        
        Predicate likePredicate = mock(Predicate.class);
        lenient().when(builder.like(stringPath, "%test%")).thenReturn(likePredicate);

        spec.toPredicate(root, query, builder);

        ArgumentCaptor<Predicate[]> predicatesCaptor = ArgumentCaptor.forClass(Predicate[].class);
        verify(builder).or(predicatesCaptor.capture());
        Predicate[] predicates = predicatesCaptor.getValue();
        assertEquals(1, predicates.length);
        assertSame(likePredicate, predicates[0]);
    }
}
