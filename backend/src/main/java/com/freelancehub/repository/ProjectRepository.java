package com.freelancehub.repository;

import com.freelancehub.entity.Project;
import com.freelancehub.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Find all projects posted by a specific client.
     * Used in the client dashboard to show "my projects".
     */
    List<Project> findByClientOrderByCreatedAtDesc(User client);

    /**
     * Find all OPEN projects - used in the freelancer browse page.
     * Pageable allows pagination (e.g., page 1, 10 items per page).
     */
    Page<Project> findByStatusOrderByCreatedAtDesc(Project.Status status, Pageable pageable);

    /**
     * Custom JPQL query - search projects by keyword in title or description.
     * JPQL uses entity/field names, not table/column names.
     */
    @Query("SELECT p FROM Project p WHERE " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Project> searchByKeyword(@Param("keyword") String keyword);

    /**
     * Count how many projects a client has posted.
     */
    long countByClient(User client);
}
