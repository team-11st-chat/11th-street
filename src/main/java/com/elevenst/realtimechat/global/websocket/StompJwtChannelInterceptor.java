package com.elevenst.realtimechat.global.websocket;

import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomErrorCode;
import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomException;
import com.elevenst.realtimechat.domain.chatroom.repository.ChatRoomParticipantRepository;
import com.elevenst.realtimechat.global.security.JwtTokenProvider;
import com.elevenst.realtimechat.global.security.token.AccessTokenBlacklist;
import com.elevenst.realtimechat.global.security.token.TokenClaims;
import com.elevenst.realtimechat.global.security.token.TokenInvalidationRegistry;
import io.jsonwebtoken.JwtException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
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
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_SESSION_ATTRIBUTE = "accessToken";
    private static final String CHATROOM_TOPIC_PREFIX = "/topic/chatrooms/";

    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenBlacklist accessTokenBlacklist;
    private final TokenInvalidationRegistry tokenInvalidationRegistry;
    private final ChatRoomParticipantRepository participantRepository;

    @Autowired
    public StompJwtChannelInterceptor(
            JwtTokenProvider jwtTokenProvider,
            AccessTokenBlacklist accessTokenBlacklist,
            TokenInvalidationRegistry tokenInvalidationRegistry,
            ChatRoomParticipantRepository participantRepository
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.accessTokenBlacklist = accessTokenBlacklist;
        this.tokenInvalidationRegistry = tokenInvalidationRegistry;
        this.participantRepository = participantRepository;
    }

    public StompJwtChannelInterceptor(
            JwtTokenProvider jwtTokenProvider,
            AccessTokenBlacklist accessTokenBlacklist,
            TokenInvalidationRegistry tokenInvalidationRegistry
    ) {
        this(jwtTokenProvider, accessTokenBlacklist, tokenInvalidationRegistry, null);
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (accessor.getCommand() != StompCommand.CONNECT) {
            authenticateConnectedSession(accessor);
            validateChatRoomSubscription(accessor);
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

    private void validateChatRoomSubscription(StompHeaderAccessor accessor) {
        if (accessor.getCommand() != StompCommand.SUBSCRIBE || participantRepository == null) {
            return;
        }

        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(CHATROOM_TOPIC_PREFIX)) {
            return;
        }

        Long chatRoomId = parseChatRoomId(destination);
        Long memberId = parseMemberId(accessor);
        if (!participantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)) {
            throw new ChatRoomException(ChatRoomErrorCode.ACCESS_DENIED);
        }
    }

    private Long parseChatRoomId(String destination) {
        String rawChatRoomId = destination.substring(CHATROOM_TOPIC_PREFIX.length());
        if (!StringUtils.hasText(rawChatRoomId) || rawChatRoomId.contains("/")) {
            throw new ChatRoomException(ChatRoomErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        try {
            return Long.valueOf(rawChatRoomId);
        } catch (NumberFormatException exception) {
            throw new ChatRoomException(ChatRoomErrorCode.CHAT_ROOM_NOT_FOUND);
        }
    }

    private Long parseMemberId(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null || !StringUtils.hasText(accessor.getUser().getName())) {
            throw new AuthenticationCredentialsNotFoundException("WebSocket authentication is required.");
        }
        return Long.valueOf(accessor.getUser().getName());
    }

    private UsernamePasswordAuthenticationToken createAuthentication(TokenClaims claims) {
        return new UsernamePasswordAuthenticationToken(
                String.valueOf(claims.memberId()),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + claims.role()))
        );
    }
}
