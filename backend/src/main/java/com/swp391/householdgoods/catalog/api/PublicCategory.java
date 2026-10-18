package com.swp391.householdgoods.catalog.api;

public record PublicCategory(long id, Long parentId, String name, String description,
        String imageUrl, boolean hasChildren) {}
