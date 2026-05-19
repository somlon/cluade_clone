package mju.capstone.ddingconnect.domain.techstack.dto.response;

import mju.capstone.ddingconnect.domain.techstack.domain.TechStack;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;

public record TechStackResponse(
        Long id,
        TechStackName name
) {
    public static TechStackResponse from(TechStack techStack) {
        return new TechStackResponse(
                techStack.getId(),
                techStack.getName()
        );
    }
}
