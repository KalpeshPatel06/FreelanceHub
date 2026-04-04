package com.freelancehub.service;

import com.freelancehub.dto.request.CreateProjectRequest;
import com.freelancehub.dto.response.ProjectResponse;
import com.freelancehub.entity.Project;
import com.freelancehub.entity.User;
import com.freelancehub.exception.ForbiddenException;
import com.freelancehub.repository.BidRepository;
import com.freelancehub.repository.ProjectRepository;
import com.freelancehub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private BidRepository bidRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    private User clientUser;
    private User freelancerUser;
    private CreateProjectRequest createRequest;

    @BeforeEach
    void setUp() {
        clientUser = User.builder()
                .id(1L)
                .name("Bob Client")
                .email("bob@client.com")
                .role(User.Role.CLIENT)
                .build();

        freelancerUser = User.builder()
                .id(2L)
                .name("Carol Freelancer")
                .email("carol@freelancer.com")
                .role(User.Role.FREELANCER)
                .build();

        createRequest = new CreateProjectRequest();
        createRequest.setTitle("Build a REST API");
        createRequest.setDescription("I need a Spring Boot REST API for my marketplace application");
        createRequest.setBudget(new BigDecimal("500.00"));
        createRequest.setDeadline(LocalDate.now().plusMonths(2));
    }

    /**
     * Helper: mock the Spring Security context so projectService.getCurrentUser()
     * returns the user we specify. This simulates a logged-in user.
     */
    private void mockSecurityContext(User user) {
        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn(user.getEmail());
        SecurityContextHolder.setContext(ctx);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("createProject() - success: CLIENT can create a project")
    void createProject_asClient_success() {
        // Arrange
        mockSecurityContext(clientUser);

        Project savedProject = Project.builder()
                .id(10L)
                .title(createRequest.getTitle())
                .description(createRequest.getDescription())
                .budget(createRequest.getBudget())
                .deadline(createRequest.getDeadline())
                .client(clientUser)
                .status(Project.Status.OPEN)
                .build();

        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

        // Act
        ProjectResponse response = projectService.createProject(createRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Build a REST API");
        assertThat(response.getClientId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(Project.Status.OPEN);

        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    @DisplayName("createProject() - failure: FREELANCER cannot create a project")
    void createProject_asFreelancer_throwsForbiddenException() {
        // Arrange: logged-in user is a FREELANCER
        mockSecurityContext(freelancerUser);

        // Act & Assert
        assertThatThrownBy(() -> projectService.createProject(createRequest))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only clients can post projects");

        // Verify no project was saved
        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteProject() - failure: non-owner cannot delete project")
    void deleteProject_notOwner_throwsForbiddenException() {
        // Arrange: project belongs to clientUser (id=1), but anotherClient (id=99) tries to delete it
        User anotherClient = User.builder()
                .id(99L)
                .email("other@client.com")
                .role(User.Role.CLIENT)
                .build();

        mockSecurityContext(anotherClient);

        Project project = Project.builder()
                .id(5L)
                .client(clientUser)   // owned by clientUser, NOT anotherClient
                .build();

        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));

        // Act & Assert
        assertThatThrownBy(() -> projectService.deleteProject(5L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("your own projects");

        verify(projectRepository, never()).delete(any());
    }
}
