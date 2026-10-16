package com.shofiniakol.common;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Uniform error body: {timestamp,status,code,message,path,details,extra}. */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldIssue> details,
        Map<String, Object> extra) {

    public record FieldIssue(String field, String message) {
    }
}
