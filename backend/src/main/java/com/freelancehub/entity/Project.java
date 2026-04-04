package com.freelancehub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Project entity - represents a job posted by a client.
 * Linked to the User who created it (client) via a foreign key.
 */
@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 20)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Budget is required")
    @DecimalMin(value = "1.00", message = "Budget must be at least $1")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal budget;

    @NotNull(message = "Deadline is required")
    @Future(message = "Deadline must be in the future")
    @Column(nullable = false)
    private LocalDate deadline;

    /**
     * The client who posted this project.
     * @ManyToOne means many projects can belong to one user.
     * @JoinColumn specifies the foreign key column name.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.OPEN;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // A project can receive many bids
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Bid> bids;

    public enum Status {
        OPEN,       // accepting bids
        IN_PROGRESS, // client selected a freelancer
        COMPLETED,  // work is done
        CANCELLED
    }
}
