package com.data.service.core.search;

import lombok.Data;
import java.util.List;

@Data
public class MetricRequest {
    private List<SearchCriteria> filters;
    private String metricType; // SUM, COUNT, AVG, MAX, MIN
    private String field; // The field to aggregate (e.g. "amount")
    private List<String> groupBy; // The fields to group by
}
