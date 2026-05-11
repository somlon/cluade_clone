package mju.capstone.ddingconnect.domain.techstack.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.techstack.dto.request.CreateTechStackRequest;
import mju.capstone.ddingconnect.domain.techstack.dto.response.TechStackResponse;

import java.util.List;

public interface TechStackService {

    TechStackResponse add(Member member, CreateTechStackRequest request);

    List<TechStackResponse> getMyTechStacks(Member member);

    void delete(Member member, Long techStackId);
}
