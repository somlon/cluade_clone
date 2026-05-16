package mju.capstone.ddingconnect.domain.techstack.service;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStack;
import mju.capstone.ddingconnect.domain.techstack.domain.repository.TechStackRepository;
import mju.capstone.ddingconnect.domain.techstack.dto.request.ReplaceTechStackRequest;
import mju.capstone.ddingconnect.domain.techstack.dto.response.TechStackResponse;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.TechStackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TechStackServiceImpl implements TechStackService {

    private final TechStackRepository techStackRepository;

    /**
     * 본인 기술 스택 전체를 요청 리스트로 교체한다.
     * deleteByMemberId 후 입력 리스트(중복 제거)만큼 save 하는 단일 트랜잭션.
     */
    @Override
    @Transactional
    public List<TechStackResponse> replace(Member member, ReplaceTechStackRequest request) {
        if (request.names() == null) {
            throw new TechStackHandler(ErrorStatus._BAD_REQUEST);
        }

        techStackRepository.deleteByMemberId(member.getId());

        return request.names().stream()
                .distinct()
                .map(name -> techStackRepository.save(TechStack.builder()
                        .member(member)
                        .name(name)
                        .build()))
                .map(TechStackResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TechStackResponse> getMyTechStacks(Member member) {
        return techStackRepository.findByMemberId(member.getId())
                .stream().map(TechStackResponse::from).toList();
    }
}
