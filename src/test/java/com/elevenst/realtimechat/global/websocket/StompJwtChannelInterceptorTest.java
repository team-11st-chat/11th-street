package com.elevenst.realtimechat.global.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomErrorCode;
import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomException;
import com.elevenst.realtimechat.global.security.JwtTokenProvider;
import com.elevenst.realtimechat.global.security.token.AccessTokenBlacklist;
import com.elevenst.realtimechat.global.security.token.TokenClaims;
import com.elevenst.realtimechat.global.security.token.TokenInvalidationRegistry;
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
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AccessTokenBlacklist accessTokenBlacklist;

    @Mock
    private TokenInvalidationRegistry tokenInvalidationRegistry;

    @Mock
    private com.elevenst.realtimechat.domain.chatroom.service.ChatRoomService chatRoomService;

    private StompJwtChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new StompJwtChannelInterceptor(
                jwtTokenProvider,
                accessTokenBlacklist,
                tokenInvalidationRegistry,
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
        given(jwtTokenProvider.parse(TOKEN)).willReturn(tokenClaims(null));
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
        verify(jwtTokenProvider).parse(TOKEN);
        verify(accessTokenBlacklist).contains("jti");
        verify(tokenInvalidationRegistry).isInvalidated(1L, tokenClaims("BUYER").issuedAt());
        assertThat(accessor.getUser()).isNotNull();
    }

    @Test
    void send_rejectsBlacklistedSessionAccessToken() {
        // given
        TokenClaims claims = tokenClaims("BUYER");
        given(jwtTokenProvider.parse(TOKEN)).willReturn(claims);
        given(accessTokenBlacklist.contains(claims.jti())).willReturn(true);
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
        given(jwtTokenProvider.parse(TOKEN)).willReturn(claims);
        given(accessTokenBlacklist.contains(claims.jti())).willReturn(false);
        given(tokenInvalidationRegistry.isInvalidated(claims.memberId(), claims.issuedAt())).willReturn(false);
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
