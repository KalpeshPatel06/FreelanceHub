package com.freelancehub.service;

import com.freelancehub.dto.request.SubmitBidRequest;
import com.freelancehub.dto.response.BidResponse;
import com.freelancehub.entity.Bid;
import com.freelancehub.entity.Project;
import com.freelancehub.entity.User;
import com.freelancehub.exception.BadRequestException;
import com.freelancehub.exception.ForbiddenException;
import com.freelancehub.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidService {

    private final BidRepository bidRepository;
    private final ProjectService projectService;  // reuse project lookup + getCurrentUser

    /**
     * Submit a bid on a project.
     * Business rules enforced:
     *   - Only FREELANCER role can bid
     *   - Cannot bid on your own project (clients can't bid)
     *   - Cannot bid twice on the same project
     *   - Can only bid on OPEN projects
     */
    @Transactional
    public BidResponse submitBid(Long projectId, SubmitBidRequest request) {
        User freelancer = projectService.getCurrentUser();
        Project project = projectService.findProjectOrThrow(projectId);

        // Rule 1: Only freelancers can bid
        if (freelancer.getRole() != User.Role.FREELANCER) {
            throw new ForbiddenException("Only freelancers can submit bids");
        }

        // Rule 2: Project must be open for bidding
        if (project.getStatus() != Project.Status.OPEN) {
            throw new BadRequestException("This project is not accepting bids");
        }

        // Rule 3: No duplicate bids
        if (bidRepository.existsByProjectAndFreelancer(project, freelancer)) {
            throw new BadRequestException("You have already submitted a bid for this project");
        }

        Bid bid = Bid.builder()
                .project(project)
                .freelancer(freelancer)
                .proposal(request.getProposal())
                .bidAmount(request.getBidAmount())
                .status(Bid.Status.PENDING)
                .build();

        Bid saved = bidRepository.save(bid);
        log.info("Bid submitted: freelancer {} on project {}", freelancer.getEmail(), projectId);

        return BidResponse.from(saved);
    }

    /**
     * Get all bids for a specific project.
     * Only the project's client can see all bids.
     */
    @Transactional(readOnly = true)
    public List<BidResponse> getBidsForProject(Long projectId) {
        User currentUser = projectService.getCurrentUser();
        Project project = projectService.findProjectOrThrow(projectId);

        // Authorization: only the client who owns the project can view all bids
        if (!project.getClient().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the project owner can view all bids");
        }

        return bidRepository.findByProjectOrderByCreatedAtDesc(project)
                .stream()
                .map(BidResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Get all bids submitted by the currently logged-in freelancer.
     */
    @Transactional(readOnly = true)
    public List<BidResponse> getMyBids() {
        User freelancer = projectService.getCurrentUser();
        return bidRepository.findByFreelancerOrderByCreatedAtDesc(freelancer)
                .stream()
                .map(BidResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Accept a bid. The client selects a winner.
     * - Accepted bid status -> ACCEPTED
     * - All other bids for this project -> REJECTED
     * - Project status -> IN_PROGRESS
     */
    @Transactional
    public BidResponse acceptBid(Long bidId) {
        User client = projectService.getCurrentUser();
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new BadRequestException("Bid not found"));

        // Only the project's client can accept a bid
        if (!bid.getProject().getClient().getId().equals(client.getId())) {
            throw new ForbiddenException("Only the project owner can accept bids");
        }

        // Reject all other bids for this project
        bidRepository.findByProjectOrderByCreatedAtDesc(bid.getProject())
                .forEach(b -> {
                    if (!b.getId().equals(bidId)) {
                        b.setStatus(Bid.Status.REJECTED);
                        bidRepository.save(b);
                    }
                });

        // Accept this bid and update project status
        bid.setStatus(Bid.Status.ACCEPTED);
        bid.getProject().setStatus(Project.Status.IN_PROGRESS);

        log.info("Bid {} accepted for project {}", bidId, bid.getProject().getId());
        return BidResponse.from(bidRepository.save(bid));
    }
}
