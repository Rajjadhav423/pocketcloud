package com.pocketcloud.project;

import com.pocketcloud.common.ConflictException;
import com.pocketcloud.common.NotFoundException;
import com.pocketcloud.project.ProjectDtos.CreateProjectRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    ProjectRepository repository;

    @InjectMocks
    ProjectService service;

    @Test
    void createDefaultsBranchToMain() {
        when(repository.existsByName("blog")).thenReturn(false);
        when(repository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        Project project = service.create(new CreateProjectRequest("blog", "https://github.com/me/blog.git", null));

        assertThat(project.getBranch()).isEqualTo("main");
        assertThat(project.getId()).isNotNull();
    }

    @Test
    void createRejectsDuplicateName() {
        when(repository.existsByName("blog")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateProjectRequest("blog", "https://x/y.git", "main")))
                .isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void getByNameThrowsWhenMissing() {
        when(repository.findByName("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByName("nope")).isInstanceOf(NotFoundException.class);
    }
}
