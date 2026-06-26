package com.elevenst.realtimechat.domain.chatroom.controller;

import com.elevenst.realtimechat.domain.chatroom.dto.ChatRoomResponse;
import com.elevenst.realtimechat.domain.chatroom.dto.ProductChatRoomCreateRequest;
import com.elevenst.realtimechat.domain.chatroom.service.ChatRoomService;
import com.elevenst.realtimechat.global.response.ApiResponse;
import com.elevenst.realtimechat.global.security.AuthenticatedMember;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chatrooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChatRoomResponse> createProductRoom(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody ProductChatRoomCreateRequest request
    ) {
        return ApiResponse.success(
                "Product chat room prepared.",
                chatRoomService.createProductRoom(member.memberId(), request.productId())
        );
    }

    @GetMapping("/products")
    public ApiResponse<List<ChatRoomResponse>> getProductRooms(
            @AuthenticationPrincipal AuthenticatedMember member
    ) {
        return ApiResponse.success(
                "Product chat rooms found.",
                chatRoomService.getProductRooms(member.memberId())
        );
    }

    @GetMapping("/products/{chatRoomId}")
    public ApiResponse<ChatRoomResponse> getProductRoom(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long chatRoomId
    ) {
        return ApiResponse.success(
                "Product chat room found.",
                chatRoomService.getProductRoom(member.memberId(), chatRoomId)
        );
    }

    @PostMapping("/cs")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChatRoomResponse> createCsRoom(
            @AuthenticationPrincipal AuthenticatedMember member
    ) {
        return ApiResponse.success(
                "CS chat room created.",
                chatRoomService.createCsRoom(member.memberId())
        );
    }

    @GetMapping("/cs")
    public ApiResponse<List<ChatRoomResponse>> getCsRooms(
            @AuthenticationPrincipal AuthenticatedMember member
    ) {
        return ApiResponse.success(
                "CS chat rooms found.",
                chatRoomService.getCsRooms(member.memberId(), member.role())
        );
    }

    @PostMapping("/cs/{chatRoomId}/accept")
    public ApiResponse<ChatRoomResponse> acceptCsRoom(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long chatRoomId
    ) {
        return ApiResponse.success(
                "CS chat room accepted.",
                chatRoomService.acceptCsRoom(member.memberId(), member.role(), chatRoomId)
        );
    }

    @PostMapping("/cs/{chatRoomId}/complete")
    public ApiResponse<ChatRoomResponse> completeCsRoom(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long chatRoomId
    ) {
        return ApiResponse.success(
                "CS chat room completed.",
                chatRoomService.completeCsRoom(member.memberId(), member.role(), chatRoomId)
        );
    }
}
