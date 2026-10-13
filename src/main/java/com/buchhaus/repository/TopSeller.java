package com.buchhaus.repository;

/** Copies sold per title, excluding cancelled orders (JPQL constructor projection). */
public record TopSeller(Long bookId, String title, Long copies) {
}
