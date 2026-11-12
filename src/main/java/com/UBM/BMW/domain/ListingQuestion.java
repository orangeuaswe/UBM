package com.UBM.BMW.domain;

import java.time.OffsetDateTime;

public record ListingQuestion(Long id, Long listingId, Long questionId, QuestionState state, String evidenceNote, OffsetDateTime updatedAt) {
}
