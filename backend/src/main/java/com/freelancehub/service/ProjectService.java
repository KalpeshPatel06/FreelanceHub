package com.freelancehub.service;

import com.freelancehub.dto.request.CreateProjectRequest;
import com.freelancehub.dto.response.ProjectResponse;
import com.freelancehub.entity.Project;
import com.freelancehub.entity.User;
import com.freelancehub.exception.BadRequestException;
import com.freelancehub.exception.ForbiddenException;
import com.freelancehub.exception.ResourceNotFoundException;
import com.freelancehub.repository.BidRepository;
import com.freelancehub.repository.ProjectRepository;
import com.freelancehub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;

    /**
     * Create a new project. Only users with CLIENT role can do this.
     * The logged-in user is automatically set as the project owner.
     */
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        User currentUser = getCurrentUser();

        // Business rule: only clients can post projects
        if (currentUser.getRole() != User.Role.CLIENT) {
            throw new ForbiddenException("Only clients can post projects");
        }

        Project project = Project.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .budget(request.getBudget())
                .deadline(request.getDeadline())
                .client(currentUser)
                .status(Project.Status.OPEN)
                .build();

        Project saved = projectRepository.save(project);
        log.info("Project created: '{}' by client {}", saved.getTitle(), currentUser.getEmail());

        return ProjectResponse.from(saved, 0L);
    }

    /**
     * Get all OPEN projects with pagination.
     * Public endpoint - no auth needed to browse.
     *
     * @param page zero-based page number
     * @param size number of results per page
     */
    @Transactional(readOnly = true)
    public Page<ProjectResponse> getAllOpenProjects(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return projectRepository
                .findByStatusOrderByCreatedAtDesc(Project.Status.OPEN, pageable)
                .map(p -> ProjectResponse.from(p, bidRepository.countByProject(p)));
    }

    /**
     * Get a single project by ID with its bid count.
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        Project project = findProjectOrThrow(id);
        long bidCount = bidRepository.countByProject(project);
        return ProjectResponse.from(project, bidCount);
    }

    /**
     * Get all projects posted by the currently logged-in client.
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getMyProjects() {
        User currentUser = getCurrentUser();
        return projectRepository.findByClientOrderByCreatedAtDesc(currentUser)
                .stream()
                .map(p -> ProjectResponse.from(p, bidRepository.countByProject(p)))
                .collect(Collectors.toList());
    }

    /**
     * Delete a project. Only the project owner (client) can delete it.
     */
    @Transactional
    public void deleteProject(Long id) {
        User currentUser = getCurrentUser();
        Project project = findProjectOrThrow(id);

        // Authorization check: is the current user the owner?
        if (!project.getClient().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only delete your own projects");
        }

        projectRepository.delete(project);
        log.info("Project {} deleted by {}", id, currentUser.getEmail());
    }

    /**
     * Search projects by keyword in title or description.
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> searchProjects(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BadRequestException("Search keyword cannot be empty");
        }
        return projectRepository.searchByKeyword(keyword.trim())
                .stream()
                .map(p -> ProjectResponse.from(p, bidRepository.countByProject(p)))
                .collect(Collectors.toList());
    }

    // ---- Helper methods ----

    public Project findProjectOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    }

    /**
     * Get the currently authenticated user from the SecurityContext.
     * Spring Security stores the logged-in user's email here after JWT validation.
     */
    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }
}
