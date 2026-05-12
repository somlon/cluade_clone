package mju.capstone.ddingconnect.domain.techstack.service;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStack;
import mju.capstone.ddingconnect.domain.techstack.domain.repository.TechStackRepository;
import mju.capstone.ddingconnect.domain.techstack.dto.request.CreateTechStackRequest;
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

    @Override
    @Transactional
    public TechStackResponse add(Member member, CreateTechStackRequest request) {
        if (techStackRepository.existsByMemberIdAndName(member.getId(), request.name())) {
            throw new TechStackHandler(ErrorStatus.TECH_STACK_DUPLICATE);
        }
        TechStack techStack = TechStack.builder()
                .member(member)
                .name(request.name())
                .build();
        return TechStackResponse.from(techStackRepository.save(techStack));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TechStackResponse> getMyTechStacks(Member member) {
        return techStackRepository.findByMemberId(member.getId())
                .stream().map(TechStackResponse::from).toList();
    }

    @Override
    @Transactional
    public void delete(Member member, Long techStackId) {
        TechStack techStack = techStackRepository.findById(techStackId)
                .orElseThrow(() -> new TechStackHandler(ErrorStatus.TECH_STACK_NOT_FOUND));

        if (!techStack.getMember().getId().equals(member.getId())) {
            throw new TechStackHandler(ErrorStatus.TECH_STACK_UNAUTHORIZED);
        }

        techStackRepository.delete(techStack);
    }
}
