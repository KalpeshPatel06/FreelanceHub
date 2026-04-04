package com.freelancehub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User entity - represents both clients and freelancers.
 * The 'role' field distinguishes between them.
 *
 * @Entity tells Hibernate this class maps to a database table.
 * @Table names the table explicitly.
 */
@Entity
@Table(name = "users",
       uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    // Stored as a bcrypt hash - never store plain passwords
    @NotBlank
    @Column(nullable = false)
    private String password;

    /**
     * Role determines what a user can do:
     *   CLIENT     -> post projects, view bids
     *   FREELANCER -> browse projects, submit bids
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // S3 URL of the profile image (optional)
    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // A client can have many projects (one-to-many)
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Project> projects;

    // A freelancer can submit many bids (one-to-many)
    @OneToMany(mappedBy = "freelancer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Bid> bids;

    public enum Role {
        CLIENT, FREELANCER
    }
}
