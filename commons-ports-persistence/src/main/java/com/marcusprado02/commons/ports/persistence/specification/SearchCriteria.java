package com.marcusprado02.commons.ports.persistence.specification;

import java.util.Arrays;
import java.util.List;

public class SearchCriteria {

    private final List<SearchFilter> filters;

    private SearchCriteria(List<SearchFilter> filters) {
        this.filters = filters;
    }

    public List<SearchFilter> filters() {
        return filters;
    }

    public static SearchCriteria of(SearchFilter... filters) {
        return new SearchCriteria(Arrays.asList(filters));
    }
}
