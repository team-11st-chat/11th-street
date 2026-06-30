package com.elevenst.realtimechat.global.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomErrorCode;
import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomException;
import com.elevenst.realtimechat.global.security.JwtTokenValidator;
import com.elevenst.realtimechat.global.security.token.TokenClaims;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

@ExtendWith(MockitoExtension.class)
class StompJwtChannelInterceptorTest {

    private static final String TOKEN = "access-token";

    @Mock
    private JwtTokenValidator jwtTokenValidator;

    @Mock
    private com.elevenst.realtimechat.domain.chatroom.service.ChatRoomService chatRoomService;

    private StompJwtChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new StompJwtChannelInterceptor(
                jwtTokenValidator,
                chatRoomService
        );
    }

    @Test
    void connect_usesAuthorizationHeaderIgnoringCase() {
        // given
        givenValidAccessToken();
        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT);
        accessor.setNativeHeader("authorization", "Bearer " + TOKEN);
        Message<?> message = message(accessor);

        // when
        interceptor.preSend(message, null);

        // then
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) accessor.getUser();
        assertThat(authentication.getName()).isEqualTo("1");
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_BUYER");
        assertThat(accessor.getSessionAttributes()).containsEntry("accessToken", TOKEN);
    }

    @Test
    void connect_rejectsMissingBearerToken() {
        // given
        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT);
        Message<?> message = message(accessor);

        // when & then
        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void connect_rejectsAccessTokenWithoutRoleClaim() {
        // given
        given(jwtTokenValidator.validate(TOKEN)).willThrow(new JwtException("Token role claim is missing"));
        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + TOKEN);
        Message<?> message = message(accessor);

        // when & then
        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void send_revalidatesSessionAccessToken() {
        // given
        givenValidAccessToken();
        StompHeaderAccessor accessor = accessor(StompCommand.SEND);
        accessor.getSessionAttributes().put("accessToken", TOKEN);
        Message<?> message = message(accessor);

        // when
        interceptor.preSend(message, null);

        // then
        verify(jwtTokenValidator).validate(TOKEN);
        assertThat(accessor.getUser()).isNotNull();
    }

    @Test
    void send_rejectsBlacklistedSessionAccessToken() {
        // given
        given(jwtTokenValidator.validate(TOKEN)).willThrow(new JwtException("Token is blacklisted"));
        StompHeaderAccessor accessor = accessor(StompCommand.SEND);
        accessor.getSessionAttributes().put("accessToken", TOKEN);
        Message<?> message = message(accessor);

        // when & then
        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void subscribe_rejectsNonParticipantChatRoomTopic() {
        // given
        givenValidAccessToken();
        willThrow(new ChatRoomException(ChatRoomErrorCode.ACCESS_DENIED))
                .given(chatRoomService).validateParticipantExistence(10L, 1L);
        StompHeaderAccessor accessor = accessor(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chatrooms/10");
        accessor.getSessionAttributes().put("accessToken", TOKEN);
        Message<?> message = message(accessor);

        // when & then
        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(ChatRoomException.class);
    }

    private void givenValidAccessToken() {
        TokenClaims claims = tokenClaims("BUYER");
        given(jwtTokenValidator.validate(TOKEN)).willReturn(claims);
    }

    private TokenClaims tokenClaims(String role) {
        return new TokenClaims(
                1L,
                "jti",
                "access",
                role,
                Instant.parse("2026-06-25T00:00:00Z"),
                Instant.parse("2026-06-25T01:00:00Z")
        );
    }

    private StompHeaderAccessor accessor(StompCommand command) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionAttributes(new HashMap<>());
        accessor.setLeaveMutable(true);
        return accessor;
    }

    private Message<?> message(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}

