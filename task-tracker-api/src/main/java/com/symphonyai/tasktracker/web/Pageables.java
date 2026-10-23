package com.symphonyai.tasktracker.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Pageables {

    private Pageables() {
    }

    /**
     * Builds a {@link Pageable} from request parameters.
     * The {@link Sort} is resolved by Spring Data from {@code ?sort=field,(asc|desc)}
     * parameters (repeatable); falls back to ascending id when absent.
     */
    public static Pageable of(int page, int size, Sort sort) {
        Sort resolved = sort == null || sort.isUnsorted() ? Sort.by("id") : sort;
        return PageRequest.of(Math.max(page, 0), Math.max(size, 1), resolved);
    }
}
