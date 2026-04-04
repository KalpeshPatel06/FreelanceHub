package com.freelancehub.controller;

import com.freelancehub.dto.request.CreateProjectRequest;
import com.freelancehub.dto.response.ApiResponse;
import com.freelancehub.dto.response.ProjectResponse;
import com.freelancehub.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ProjectController - REST API for project management.
 *
 * Public endpoints (no auth):
 *   GET  /api/projects         - browse all open projects
 *   GET  /api/projects/{id}    - view a project
 *   GET  /api/projects/search  - search projects
 *
 * Protected endpoints (require JWT):
 *   POST   /api/projects        - create project (CLIENT only)
 *   GET    /api/projects/my     - get my projects (CLIENT)
 *   DELETE /api/projects/{id}   - delete project (owner CLIENT)
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * GET /api/projects?page=0&size=10
     * Browse all open projects with pagination.
     * PUBLIC - no authentication required.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProjectResponse>>> getAllProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ProjectResponse> projects = projectService.getAllOpenProjects(page, size);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    /**
     * GET /api/projects/{id}
     * Get a single project's details.
     * PUBLIC - no authentication required.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectById(
            @PathVariable Long id) {

        ProjectResponse project = projectService.getProjectById(id);
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    /**
     * GET /api/projects/search?keyword=logo
     * Search projects by keyword.
     * PUBLIC - no authentication required.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> searchProjects(
            @RequestParam String keyword) {

        List<ProjectResponse> results = projectService.searchProjects(keyword);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * POST /api/projects
     * Create a new project.
     * PROTECTED - requires CLIENT role.
     *
     * @PreAuthorize checks the user's role BEFORE entering the method.
     * If the role doesn't match, Spring returns 403 Forbidden automatically.
     */
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody CreateProjectRequest request) {

        ProjectResponse project = projectService.createProject(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project created successfully", project));
    }

    /**
     * GET /api/projects/my
     * Get all projects posted by the currently logged-in client.
     * PROTECTED - requires CLIENT role.
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getMyProjects() {
        List<ProjectResponse> projects = projectService.getMyProjects();
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    /**
     * DELETE /api/projects/{id}
     * Delete a project (only the owner can delete).
     * PROTECTED - requires CLIENT role (ownership checked in service).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok(ApiResponse.success("Project deleted successfully", null));
    }
}
