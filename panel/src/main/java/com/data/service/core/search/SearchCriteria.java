package com.data.service.core.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteria {
    @JsonProperty("field")
    private String key;
    private SearchOperation operation;
    private Object value;

    @JsonSetter("operator")
    public void setOperator(String operator) {
        if (operator == null) return;
        switch (operator.toUpperCase()) {
            case "=": this.operation = SearchOperation.EQUALITY; break;
            case "!=": this.operation = SearchOperation.NEGATION; break;
            case ">": this.operation = SearchOperation.GREATER_THAN; break;
            case ">=": this.operation = SearchOperation.GREATER_THAN_EQUAL; break;
            case "<": this.operation = SearchOperation.LESS_THAN; break;
            case "<=": this.operation = SearchOperation.LESS_THAN_EQUAL; break;
            case "LIKE": this.operation = SearchOperation.LIKE; break;
            case "IN": this.operation = SearchOperation.IN; break;
            default: this.operation = SearchOperation.EQUALITY;
        }
    }
}
