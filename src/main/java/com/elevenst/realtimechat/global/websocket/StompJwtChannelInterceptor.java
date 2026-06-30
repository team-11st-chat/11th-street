package com.elevenst.realtimechat.global.websocket;

import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomErrorCode;
import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomException;
import com.elevenst.realtimechat.domain.chatroom.service.ChatRoomService;
import com.elevenst.realtimechat.global.security.JwtTokenValidator;
import com.elevenst.realtimechat.global.security.ValidatedToken;
import com.elevenst.realtimechat.global.security.token.TokenClaims;
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
    private static final String CHATROOM_TOPIC_PREFIX = "/topic/chatrooms/";

    private final JwtTokenValidator jwtTokenValidator;
    private final ChatRoomService chatRoomService;

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
        ValidatedToken validatedToken = authenticate(token);
        setAccessToken(accessor, token);
        accessor.setUser(createAuthentication(validatedToken));
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

    private ValidatedToken authenticate(String token) {
        try {
            return jwtTokenValidator.validate(token);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BadCredentialsException("Invalid access token.", exception);
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

        ValidatedToken validatedToken = authenticate(accessToken);
        accessor.setUser(createAuthentication(validatedToken));
    }

    private boolean requiresSessionAuthentication(StompCommand command) {
        return command == StompCommand.SEND
                || command == StompCommand.SUBSCRIBE
                || command == StompCommand.UNSUBSCRIBE
                || command == StompCommand.ACK
                || command == StompCommand.NACK;
    }

    private void validateChatRoomSubscription(StompHeaderAccessor accessor) {
        if (accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return;
        }

        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(CHATROOM_TOPIC_PREFIX)) {
            return;
        }

        Long chatRoomId = parseChatRoomId(destination);
        Long memberId = parseMemberId(accessor);
        chatRoomService.validateParticipantExistence(chatRoomId, memberId);
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

    private UsernamePasswordAuthenticationToken createAuthentication(ValidatedToken validatedToken) {
        return new UsernamePasswordAuthenticationToken(
                String.valueOf(validatedToken.getMemberId()),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + validatedToken.role().name()))
        );
    }
}
