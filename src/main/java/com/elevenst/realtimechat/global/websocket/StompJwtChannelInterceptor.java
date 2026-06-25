package com.elevenst.realtimechat.global.websocket;

import com.elevenst.realtimechat.global.security.JwtTokenProvider;
import com.elevenst.realtimechat.global.security.token.AccessTokenBlacklist;
import com.elevenst.realtimechat.global.security.token.TokenClaims;
import com.elevenst.realtimechat.global.security.token.TokenInvalidationRegistry;
import io.jsonwebtoken.JwtException;
import java.util.List;
import java.util.Map;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_SESSION_ATTRIBUTE = "accessToken";

    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenBlacklist accessTokenBlacklist;
    private final TokenInvalidationRegistry tokenInvalidationRegistry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (accessor.getCommand() != StompCommand.CONNECT) {
            authenticateConnectedSession(accessor);
            return message;
        }

        String token = extractBearerToken(accessor);
        TokenClaims claims = authenticate(token);
        setAccessToken(accessor, token);
        accessor.setUser(createAuthentication(claims));
        return message;
    }

    private String extractBearerToken(StompHeaderAccessor accessor) {
        String authorization = getFirstNativeHeaderIgnoreCase(accessor, AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new AuthenticationCredentialsNotFoundException("Authorization Bearer token is required.");
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new AuthenticationCredentialsNotFoundException("Authorization Bearer token is required.");
        }
        return token;
    }

    private String getFirstNativeHeaderIgnoreCase(StompHeaderAccessor accessor, String headerName) {
        String directValue = accessor.getFirstNativeHeader(headerName);
        if (directValue != null) {
            return directValue;
        }

        for (Map.Entry<String, List<String>> entry : accessor.toNativeHeaderMap().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(headerName) && !entry.getValue().isEmpty()) {
                return entry.getValue().get(0);
            }
        }
        return null;
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
        if (!claims.isAccess() || !StringUtils.hasText(claims.jti()) || !StringUtils.hasText(claims.role())) {
            throw new BadCredentialsException("Invalid access token.");
        }
        if (accessTokenBlacklist.contains(claims.jti())) {
            throw new BadCredentialsException("Invalid access token.");
        }
        if (tokenInvalidationRegistry.isInvalidated(claims.memberId(), claims.issuedAt())) {
            throw new BadCredentialsException("Invalid access token.");
        }
    }

    private void setAccessToken(StompHeaderAccessor accessor, String token) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new AuthenticationCredentialsNotFoundException("WebSocket session is required.");
        }
        sessionAttributes.put(ACCESS_TOKEN_SESSION_ATTRIBUTE, token);
    }

    private void authenticateConnectedSession(StompHeaderAccessor accessor) {
        if (!requiresSessionAuthentication(accessor.getCommand())) {
            return;
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new AuthenticationCredentialsNotFoundException("WebSocket session is required.");
        }

        Object token = sessionAttributes.get(ACCESS_TOKEN_SESSION_ATTRIBUTE);
        if (!(token instanceof String accessToken) || !StringUtils.hasText(accessToken)) {
            throw new AuthenticationCredentialsNotFoundException("Authorization Bearer token is required.");
        }

        TokenClaims claims = authenticate(accessToken);
        accessor.setUser(createAuthentication(claims));
    }

    private boolean requiresSessionAuthentication(StompCommand command) {
        return command == StompCommand.SEND
                || command == StompCommand.SUBSCRIBE
                || command == StompCommand.UNSUBSCRIBE
                || command == StompCommand.ACK
                || command == StompCommand.NACK;
    }

    private UsernamePasswordAuthenticationToken createAuthentication(TokenClaims claims) {
        return new UsernamePasswordAuthenticationToken(
                String.valueOf(claims.memberId()),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + claims.role()))
        );
    }
}
