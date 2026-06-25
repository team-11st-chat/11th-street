package com.elevenst.realtimechat.global.websocket;

import com.elevenst.realtimechat.global.security.JwtTokenProvider;
import com.elevenst.realtimechat.global.security.token.AccessTokenBlacklist;
import com.elevenst.realtimechat.global.security.token.TokenClaims;
import com.elevenst.realtimechat.global.security.token.TokenInvalidationRegistry;
import io.jsonwebtoken.JwtException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenBlacklist accessTokenBlacklist;
    private final TokenInvalidationRegistry tokenInvalidationRegistry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String token = extractBearerToken(accessor);
        TokenClaims claims = authenticate(token);
        accessor.setUser(createAuthentication(claims));
        return message;
    }

    private String extractBearerToken(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new AuthenticationCredentialsNotFoundException("Authorization Bearer token is required.");
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new AuthenticationCredentialsNotFoundException("Authorization Bearer token is required.");
        }
        return token;
    }

    private TokenClaims authenticate(String token) {
        try {
            TokenClaims claims = jwtTokenProvider.parse(token);
            validateAccessToken(claims);
            return claims;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BadCredentialsException("Invalid access token.", exception);
        }
    }

    private void validateAccessToken(TokenClaims claims) {
        if (!claims.isAccess() || !StringUtils.hasText(claims.jti())) {
            throw new BadCredentialsException("Invalid access token.");
        }
        if (accessTokenBlacklist.contains(claims.jti())) {
            throw new BadCredentialsException("Invalid access token.");
        }
        if (tokenInvalidationRegistry.isInvalidated(claims.memberId(), claims.issuedAt())) {
            throw new BadCredentialsException("Invalid access token.");
        }
    }

    private UsernamePasswordAuthenticationToken createAuthentication(TokenClaims claims) {
        return new UsernamePasswordAuthenticationToken(String.valueOf(claims.memberId()), null, List.of());
    }
}
