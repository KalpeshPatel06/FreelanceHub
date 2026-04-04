package com.freelancehub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bid entity - a freelancer's proposal on a project.
 * Links a freelancer (User) to a Project.
 */
@Entity
@Table(name = "bids",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"project_id", "freelancer_id"},
           name = "uq_bid_project_freelancer"  // one bid per freelancer per project
       ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The project this bid is for.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /**
     * The freelancer who submitted this bid.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private User freelancer;

    @NotBlank(message = "Proposal is required")
    @Size(min = 20, message = "Proposal must be at least 20 characters")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String proposal;

    @NotNull(message = "Bid amount is required")
    @DecimalMin(value = "1.00", message = "Bid amount must be at least $1")
    @Column(name = "bid_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal bidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Status {
        PENDING,   // waiting for client decision
        ACCEPTED,  // client chose this bid
        REJECTED   // client rejected this bid
    }
}
