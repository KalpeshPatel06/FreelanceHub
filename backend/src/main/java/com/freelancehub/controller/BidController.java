package com.freelancehub.controller;

import com.freelancehub.dto.request.SubmitBidRequest;
import com.freelancehub.dto.response.ApiResponse;
import com.freelancehub.dto.response.BidResponse;
import com.freelancehub.service.BidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * BidController - REST API for bid management.
 *
 * Endpoints:
 *   POST /api/bids/projects/{projectId}          - submit a bid (FREELANCER)
 *   GET  /api/bids/projects/{projectId}          - get bids for a project (CLIENT/owner)
 *   GET  /api/bids/my                            - get my bids (FREELANCER)
 *   PUT  /api/bids/{bidId}/accept                - accept a bid (CLIENT)
 */
@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    /**
     * POST /api/bids/projects/{projectId}
     * Submit a bid on a project.
     * PROTECTED - requires FREELANCER role.
     */
    @PostMapping("/projects/{projectId}")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<ApiResponse<BidResponse>> submitBid(
            @PathVariable Long projectId,
            @Valid @RequestBody SubmitBidRequest request) {

        BidResponse bid = bidService.submitBid(projectId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bid submitted successfully", bid));
    }

    /**
     * GET /api/bids/projects/{projectId}
     * Get all bids for a project (project owner only).
     * PROTECTED - requires CLIENT role, service enforces ownership.
     */
    @GetMapping("/projects/{projectId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<List<BidResponse>>> getBidsForProject(
            @PathVariable Long projectId) {

        List<BidResponse> bids = bidService.getBidsForProject(projectId);
        return ResponseEntity.ok(ApiResponse.success(bids));
    }

    /**
     * GET /api/bids/my
     * Get all bids submitted by the logged-in freelancer.
     * PROTECTED - requires FREELANCER role.
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<ApiResponse<List<BidResponse>>> getMyBids() {
        List<BidResponse> bids = bidService.getMyBids();
        return ResponseEntity.ok(ApiResponse.success(bids));
    }

    /**
     * PUT /api/bids/{bidId}/accept
     * Accept a bid (sets winner, rejects all others, updates project status).
     * PROTECTED - requires CLIENT role, service enforces ownership.
     */
    @PutMapping("/{bidId}/accept")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<BidResponse>> acceptBid(
            @PathVariable Long bidId) {

        BidResponse bid = bidService.acceptBid(bidId);
        return ResponseEntity.ok(ApiResponse.success("Bid accepted successfully", bid));
    }
}
