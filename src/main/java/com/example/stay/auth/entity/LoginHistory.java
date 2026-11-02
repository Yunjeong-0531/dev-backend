package com.example.stay.auth.entity;

import com.example.stay.auth.enums.LoginMethod;
import com.example.stay.common.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "login_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginHistory extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 존재하지 않는 이메일로 시도한 경우 null (FK 없이 id 만 보관) */
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginMethod method;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "fail_reason", length = 50)
    private String failReason;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    public static LoginHistory success(Long userId, String email, LoginMethod method,
                                       String ipAddress, String userAgent) {
        LoginHistory h = new LoginHistory();
        h.userId = userId;
        h.email = email;
        h.method = method;
        h.success = true;
        h.ipAddress = ipAddress;
        h.userAgent = userAgent;
        return h;
    }

    public static LoginHistory fail(Long userId, String email, LoginMethod method,
                                    String failReason, String ipAddress, String userAgent) {
        LoginHistory h = new LoginHistory();
        h.userId = userId;
        h.email = email;
        h.method = method;
        h.success = false;
        h.failReason = failReason;
        h.ipAddress = ipAddress;
        h.userAgent = userAgent;
        return h;
    }
}
