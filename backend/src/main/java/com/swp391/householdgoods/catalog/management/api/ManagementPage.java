package com.swp391.householdgoods.catalog.management.api;

import java.util.List;

/** One item per master product. Load its detail to review/edit the associated SKUs. */
public record ManagementPage(List<ManagedProduct> items, int page, int size, long totalElements) {}
