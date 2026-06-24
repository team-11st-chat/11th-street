package com.elevenst.realtimechat.global.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * STUB: deny-by-default 보안 필터체인과 들어오는 JWT 인증 필터는 보안/플랫폼 공통 작업 단위 소유로, 이슈 #21 범위 밖이다.
 * 현재는 회원가입·로그인 엔드포인트가 동작하도록 모든 요청을 permitAll 하는 임시 구현이다.
 *
 * TODO(실제 구현 교체 지점): 화이트리스트(회원가입·로그인) 외 deny-by-default 적용,
 *   JWT 인증 필터 추가, STATELESS 세션 정책으로 교체.
 *
 * 참고: PasswordEncoder(BCrypt)는 이슈 deliverable("비밀번호 해시")이므로 실제 구현을 유지한다.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // STUB: 임시로 전체 허용. 실제 보안 정책으로 교체 필요.
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
