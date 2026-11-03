package com.example.stay.common.security;

import com.example.stay.common.CommonErrorCode;
import com.example.stay.common.ErrorCode;
import com.example.stay.common.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

/**
 * 전체 서비스 공용 보안 설정. 경로별 공개/인증 규칙은 이 파일에 하드코딩하지 않고
 * 각 feature 가 SecurityRuleContributor 구현체로 등록한다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final SecurityProperties props;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<SecurityRuleContributor> contributors;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        List<SecurityRuleContributor> rules = contributors.orderedStream().toList();

        http
                .csrf(AbstractHttpConfigurer::disable)           // Bearer 토큰 방식(쿠키 인증 아님)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    // 1) "공개 패턴 안의 예외(로그인 필요)" 를 먼저 등록해야 아래 공개 규칙에 가려지지 않는다
                    rules.stream().flatMap(r -> r.authenticatedPaths().stream())
                            .forEach(path -> auth.requestMatchers(path).authenticated());
                    // 2) 각 feature 의 공개 경로
                    rules.stream().flatMap(r -> r.publicEndpoints().stream()).forEach(e -> {
                        if (e.method() == null) {
                            auth.requestMatchers(e.pattern()).permitAll();
                        } else {
                            auth.requestMatchers(e.method(), e.pattern()).permitAll();
                        }
                    });
                    // 3) 나머지는 모두 로그인 필요
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> writeError(res, CommonErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((req, res, ex) -> writeError(res, CommonErrorCode.FORBIDDEN)))
                // 필터는 @Component 로 등록하지 않고 여기서만 생성한다 (서블릿 필터 이중 등록 방지)
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /** Boot 가 임시 user/비밀번호를 만들어 로그에 찍는 것을 막기 위한 빈 (실제 인증은 auth feature 에서 직접 처리) */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("not supported");
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(props.allowedOrigins());
        c.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Device-Name"));
        c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", c);
        return source;
    }

    private void writeError(HttpServletResponse res, ErrorCode code) throws IOException {
        res.setStatus(code.getStatus().value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getWriter(), ErrorResponse.of(code));
    }
}
