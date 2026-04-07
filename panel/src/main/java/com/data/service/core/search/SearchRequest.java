package com.data.service.core.search;

import lombok.Data;
import java.util.List;

@Data
public class SearchRequest {
    private List<SearchCriteria> conditions;
}
