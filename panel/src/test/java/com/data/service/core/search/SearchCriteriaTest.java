package com.data.service.core.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SearchCriteriaTest {

    @Test
    void testConstructorAndGetters() {
        SearchCriteria criteria = new SearchCriteria("field", SearchOperation.EQUALITY, "value");

        assertEquals("field", criteria.getKey());
        assertEquals(SearchOperation.EQUALITY, criteria.getOperation());
        assertEquals("value", criteria.getValue());
    }

    @Test
    void testSetters() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setKey("newKey");
        criteria.setOperation(SearchOperation.GREATER_THAN);
        criteria.setValue(100);

        assertEquals("newKey", criteria.getKey());
        assertEquals(SearchOperation.GREATER_THAN, criteria.getOperation());
        assertEquals(100, criteria.getValue());
    }

    @Test
    void testNoArgsConstructor() {
        SearchCriteria criteria = new SearchCriteria();
        assertNull(criteria.getKey());
        assertNull(criteria.getOperation());
        assertNull(criteria.getValue());
    }
}
