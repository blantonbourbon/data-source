package com.data.service.core.search;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MetricRequestTest {

    @Test
    void testGettersAndSetters() {
        MetricRequest request = new MetricRequest();
        request.setMetricType("COUNT");
        request.setField("id");
        request.setFilters(Collections.emptyList());

        assertEquals("COUNT", request.getMetricType());
        assertEquals("id", request.getField());
        assertEquals(0, request.getFilters().size());
    }

    @Test
    void testInitialState() {
        MetricRequest request = new MetricRequest();
        assertNull(request.getMetricType());
        assertNull(request.getField());
        assertNull(request.getFilters());
    }
}
