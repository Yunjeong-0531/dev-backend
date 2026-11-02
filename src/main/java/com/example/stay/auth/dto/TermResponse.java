package com.example.stay.auth.dto;

import com.example.stay.auth.entity.Term;

public record TermResponse(
        Long id, String type, String version, String title,
        String content, boolean required, int displayOrder
) {
    public static TermResponse from(Term t) {
        return new TermResponse(t.getId(), t.getType(), t.getVersion(), t.getTitle(),
                t.getContent(), t.isRequired(), t.getDisplayOrder());
    }
}
