package com.freelancehub.dto.response;

import com.freelancehub.entity.Bid;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BidResponse {

    private Long id;
    private Long projectId;
    private String projectTitle;
    private Long freelancerId;
    private String freelancerName;
    private String freelancerProfileImageUrl;
    private String proposal;
    private BigDecimal bidAmount;
    private Bid.Status status;
    private LocalDateTime createdAt;

    public static BidResponse from(Bid bid) {
        return BidResponse.builder()
                .id(bid.getId())
                .projectId(bid.getProject().getId())
                .projectTitle(bid.getProject().getTitle())
                .freelancerId(bid.getFreelancer().getId())
                .freelancerName(bid.getFreelancer().getName())
                .freelancerProfileImageUrl(bid.getFreelancer().getProfileImageUrl())
                .proposal(bid.getProposal())
                .bidAmount(bid.getBidAmount())
                .status(bid.getStatus())
                .createdAt(bid.getCreatedAt())
                .build();
    }
}
