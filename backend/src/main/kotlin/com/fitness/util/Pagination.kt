package com.fitness.util

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

fun pageRequest(page: Int, size: Int, sort: Sort = Sort.unsorted(), maxSize: Int = 100): PageRequest {
    return PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, maxSize), sort)
}
