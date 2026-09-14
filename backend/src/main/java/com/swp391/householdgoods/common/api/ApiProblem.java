package com.swp391.householdgoods.common.api;

import java.util.Map;

public record ApiProblem(
        String type,
        String title,
        int status,
        String code,
        String detail,
        Map<String, String> fieldErrors,
        String traceId) {

    public static ApiProblem of(
            int status,
            String title,
            String code,
            String detail,
            Map<String, String> fieldErrors,
            String traceId) {
        return new ApiProblem("about:blank", title, status, code, detail, fieldErrors, traceId);
    }
}
