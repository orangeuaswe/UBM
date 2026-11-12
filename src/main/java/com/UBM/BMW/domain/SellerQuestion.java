package com.UBM.BMW.domain;
import java.util.List;
public record SellerQuestion(Long id, Long issueId, String slug, String question, String whyMatter, String goodAnswer, String redFlagAnswer, List<String> evidenceRequired, List<String> photoRequest, String inspectionMethod, boolean recommendSpecialistPPI)
{
}
