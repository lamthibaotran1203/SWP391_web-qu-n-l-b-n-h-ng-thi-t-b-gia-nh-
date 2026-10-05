package com.swp391.householdgoods.catalog.api;

import java.util.List;
public record CatalogPage(List<CatalogSku> items, int page, int size, long totalElements) {}
