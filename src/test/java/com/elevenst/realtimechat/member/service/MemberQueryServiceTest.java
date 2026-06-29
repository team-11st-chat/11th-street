package com.elevenst.realtimechat.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.elevenst.realtimechat.domain.member.entity.Member;
import com.elevenst.realtimechat.domain.member.repository.MemberRepository;
import com.elevenst.realtimechat.domain.member.service.MemberQueryService;
import com.elevenst.realtimechat.global.exception.BusinessException;
import com.elevenst.realtimechat.global.exception.CommonErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberQueryServiceTest {

    @Mock
    private MemberRepository memberRepository;

    private MemberQueryService memberQueryService;

    @BeforeEach
    void setUp() {
        memberQueryService = new MemberQueryService(memberRepository);
    }

    @Test
    void 회원이_존재하면_회원_엔티티를_반환한다() {
        Member member = Member.create("buyer@example.com", "passwordHash", "구매자");
        ReflectionTestUtils.setField(member, "id", 1L);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        Member result = memberQueryService.getMemberOrThrow(1L);

        assertThat(result).isSameAs(member);
    }

    @Test
    void 회원이_존재하지_않으면_잘못된_요청_예외가_발생한다() {
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberQueryService.getMemberOrThrow(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(CommonErrorCode.INVALID_REQUEST);
    }
}
