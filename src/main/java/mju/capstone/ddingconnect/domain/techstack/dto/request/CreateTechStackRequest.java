package mju.capstone.ddingconnect.domain.techstack.dto.request;

import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;

public record CreateTechStackRequest(
        TechStackName name
) {}
