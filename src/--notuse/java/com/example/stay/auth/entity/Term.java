package com.example.stay.auth.entity;

import com.example.stay.common.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "terms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Term extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SERVICE, PRIVACY, MARKETING, AGE_14 ... */
    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "effective_at", nullable = false)
    private LocalDateTime effectiveAt;

    public static Term create(String type, String version, String title, String content,
                              boolean required, int displayOrder, LocalDateTime effectiveAt) {
        Term t = new Term();
        t.type = type;
        t.version = version;
        t.title = title;
        t.content = content;
        t.required = required;
        t.displayOrder = displayOrder;
        t.effectiveAt = effectiveAt;
        t.active = true;
        return t;
    }

    public void deactivate() {
        this.active = false;
    }
}
