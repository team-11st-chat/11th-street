package com.elevenst.realtimechat.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.elevenst.realtimechat.domain.member.entity.Member;
import com.elevenst.realtimechat.domain.member.service.MemberQueryService;
import com.elevenst.realtimechat.domain.order.dto.TimeSaleOrderRequest;
import com.elevenst.realtimechat.domain.order.dto.TimeSaleOrderResponse;
import com.elevenst.realtimechat.domain.order.entity.TimeSaleOrder;
import com.elevenst.realtimechat.domain.order.entity.TimeSaleOrderStatus;
import com.elevenst.realtimechat.domain.order.repository.TimeSaleOrderRepository;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleErrorCode;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleException;
import com.elevenst.realtimechat.domain.promotion.service.TimeSalePurchaseService;
import com.elevenst.realtimechat.domain.promotion.service.TimeSalePurchaseSnapshot;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TimeSaleOrderServiceTest {

    @Mock
    private TimeSaleOrderRepository timeSaleOrderRepository;

    @Mock
    private MemberQueryService memberQueryService;

    @Mock
    private TimeSalePurchaseService timeSalePurchaseService;

    private TimeSaleOrderService timeSaleOrderService;

    @BeforeEach
    void setUp() {
        timeSaleOrderService = new TimeSaleOrderService(
                timeSaleOrderRepository,
                memberQueryService,
                timeSalePurchaseService
        );
    }

    @Test
    void 타임세일_주문이_가능하면_회원과_타임세일_스냅샷으로_주문을_저장한다() {
        Long memberId = 1L;
        Long timeSaleId = 10L;
        String requestId = "request-1";
        Member member = createMember(memberId);
        TimeSalePurchaseSnapshot snapshot = new TimeSalePurchaseSnapshot(
                100L,
                timeSaleId,
                "Time sale product",
                new BigDecimal("10000"),
                new BigDecimal("9000")
        );
        given(memberQueryService.getMemberOrThrow(memberId)).willReturn(member);
        given(timeSaleOrderRepository.existsByMemberIdAndTimeSaleIdAndStatus(
                memberId, timeSaleId, TimeSaleOrderStatus.COMPLETED)).willReturn(false);
        given(timeSalePurchaseService.purchase(any(), any(Integer.class), any())).willReturn(snapshot);
        given(timeSaleOrderRepository.save(any(TimeSaleOrder.class))).willAnswer(invocation -> invocation.getArgument(0));

        TimeSaleOrderResponse response = timeSaleOrderService.orderTimeSale(
                memberId,
                timeSaleId,
                requestId,
                new TimeSaleOrderRequest(2)
        );

        ArgumentCaptor<TimeSaleOrder> orderCaptor = ArgumentCaptor.forClass(TimeSaleOrder.class);
        verify(timeSaleOrderRepository).save(orderCaptor.capture());
        TimeSaleOrder savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getMember()).isSameAs(member);
        assertThat(savedOrder.getProductId()).isEqualTo(100L);
        assertThat(savedOrder.getTimeSaleId()).isEqualTo(timeSaleId);
        assertThat(savedOrder.getRequestId()).isEqualTo(requestId);
        assertThat(savedOrder.getProductNameSnapshot()).isEqualTo("Time sale product");
        assertThat(savedOrder.getOriginalPriceSnapshot()).isEqualByComparingTo("10000");
        assertThat(savedOrder.getSalePriceSnapshot()).isEqualByComparingTo("9000");
        assertThat(savedOrder.getQuantity()).isEqualTo(2);
        assertThat(savedOrder.getStatus()).isEqualTo(TimeSaleOrderStatus.COMPLETED);
        assertThat(response.timeSaleId()).isEqualTo(timeSaleId);
        assertThat(response.productId()).isEqualTo(100L);
        assertThat(response.quantity()).isEqualTo(2);

        InOrder inOrder = inOrder(timeSalePurchaseService, timeSaleOrderRepository);
        inOrder.verify(timeSalePurchaseService).validatePurchasable(any(), any());
        inOrder.verify(timeSaleOrderRepository).existsByMemberIdAndTimeSaleIdAndStatus(
                memberId, timeSaleId, TimeSaleOrderStatus.COMPLETED);
        inOrder.verify(timeSalePurchaseService).purchase(any(), any(Integer.class), any());
    }

    @Test
    void 이미_완료된_타임세일_주문이_있으면_구매_처리를_호출하지_않고_중복_예외가_발생한다() {
        Long memberId = 1L;
        Long timeSaleId = 10L;
        Member member = createMember(memberId);
        given(memberQueryService.getMemberOrThrow(memberId)).willReturn(member);
        given(timeSaleOrderRepository.existsByMemberIdAndTimeSaleIdAndStatus(
                memberId, timeSaleId, TimeSaleOrderStatus.COMPLETED)).willReturn(true);

        assertThatThrownBy(() -> timeSaleOrderService.orderTimeSale(
                memberId,
                timeSaleId,
                "request-1",
                new TimeSaleOrderRequest(1)
        ))
                .isInstanceOf(TimeSaleException.class)
                .extracting(exception -> ((TimeSaleException) exception).getErrorCode())
                .isEqualTo(TimeSaleErrorCode.TIME_SALE_003);

        verify(timeSalePurchaseService, never()).purchase(any(), any(Integer.class), any());
        verify(timeSaleOrderRepository, never()).save(any());
    }

    private Member createMember(Long memberId) {
        Member member = Member.create("buyer@example.com", "passwordHash", "구매자");
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }
}
