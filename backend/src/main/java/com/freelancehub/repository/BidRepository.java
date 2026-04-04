package com.freelancehub.repository;

import com.freelancehub.entity.Bid;
import com.freelancehub.entity.Project;
import com.freelancehub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    /**
     * Get all bids for a specific project (used on project detail page).
     */
    List<Bid> findByProjectOrderByCreatedAtDesc(Project project);

    /**
     * Get all bids submitted by a freelancer (used in freelancer dashboard).
     */
    List<Bid> findByFreelancerOrderByCreatedAtDesc(User freelancer);

    /**
     * Check if a freelancer already bid on a project.
     * Enforces the unique constraint at the application layer too.
     */
    boolean existsByProjectAndFreelancer(Project project, User freelancer);

    /**
     * Find a specific bid by project and freelancer.
     */
    Optional<Bid> findByProjectAndFreelancer(Project project, User freelancer);

    /**
     * Count how many bids a project has received.
     */
    long countByProject(Project project);
}
