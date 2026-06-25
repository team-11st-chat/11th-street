package com.elevenst.realtimechat.global.security;

import com.elevenst.realtimechat.domain.member.entity.MemberRole;
import com.elevenst.realtimechat.global.security.token.AccessTokenBlacklist;
import com.elevenst.realtimechat.global.security.token.TokenClaims;
import com.elevenst.realtimechat.global.security.token.TokenInvalidationRegistry;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization 헤더의 Bearer Access Token 을 검증해 SecurityContext 에 인증 정보를 채우는 필터.
 *
 * <p>서명·만료·위조 검증({@link JwtTokenProvider#parse})에 실패하거나, Access Token 이 아니거나,
 * 로그아웃(블랙리스트)·사용자 무효화 기준에 걸리면 인증을 설정하지 않고 통과시킨다. 이후
 * 보호 경로에서는 인증 정보가 없으므로 {@link RestAuthenticationEntryPoint} 가 401 로 거부한다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenBlacklist accessTokenBlacklist;
    private final TokenInvalidationRegistry tokenInvalidationRegistry;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            AccessTokenBlacklist accessTokenBlacklist,
            TokenInvalidationRegistry tokenInvalidationRegistry) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.accessTokenBlacklist = accessTokenBlacklist;
        this.tokenInvalidationRegistry = tokenInvalidationRegistry;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveBearerToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(token);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        TokenClaims claims;
        try {
            claims = jwtTokenProvider.parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            // 서명·만료·위조 검증 실패: 인증을 설정하지 않고 진입점이 거부하도록 둔다.
            return;
        }
        if (!claims.isAccess() || claims.role() == null) {
            return;
        }
        if (accessTokenBlacklist.contains(claims.jti())) {
            return;
        }
        if (tokenInvalidationRegistry.isInvalidated(claims.memberId(), claims.issuedAt())) {
            return;
        }

        MemberRole role;
        try {
            role = MemberRole.valueOf(claims.role());
        } catch (IllegalArgumentException e) {
            // 알 수 없는 role 클레임: 무효 토큰으로 보고 인증을 설정하지 않는다(진입점이 401 로 거부).
            return;
        }
        AuthenticatedMember principal = new AuthenticatedMember(claims.memberId(), role);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
