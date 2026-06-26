package com.elevenst.realtimechat.domain.message.controller;

import com.elevenst.realtimechat.domain.message.dto.ChatMessageRequest;
import com.elevenst.realtimechat.domain.message.service.ChatMessageService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @MessageMapping("/chatrooms/{chatRoomId}/messages")
    public void sendMessage(
            @DestinationVariable Long chatRoomId,
            @Valid @Payload ChatMessageRequest request,
            Principal principal
    ) {
        chatMessageService.sendMessage(chatRoomId, resolveMemberId(principal), request);
    }

    private Long resolveMemberId(Principal principal) {
        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException("WebSocket authentication is required.");
        }
        return Long.valueOf(principal.getName());
    }
}
