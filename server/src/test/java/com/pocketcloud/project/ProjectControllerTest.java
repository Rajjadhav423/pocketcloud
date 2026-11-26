package com.pocketcloud.project;

import com.pocketcloud.common.ConflictException;
import com.pocketcloud.common.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProjectService service;

    @Test
    void createReturns201WithLocation() throws Exception {
        when(service.create(any())).thenReturn(new Project("blog", "https://github.com/me/blog.git", "main"));

        mvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"blog","repoUrl":"https://github.com/me/blog.git"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/projects/blog"))
                .andExpect(jsonPath("$.name").value("blog"))
                .andExpect(jsonPath("$.branch").value("main"));
    }

    @Test
    void createRejectsInvalidName() throws Exception {
        mvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"My_Blog","repoUrl":"https://github.com/me/blog.git"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void createReturns409OnDuplicate() throws Exception {
        when(service.create(any())).thenThrow(new ConflictException("Project 'blog' already exists"));

        mvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"blog","repoUrl":"https://github.com/me/blog.git"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void getReturns404WhenMissing() throws Exception {
        when(service.getByName("nope")).thenThrow(new NotFoundException("Project 'nope' not found"));

        mvc.perform(get("/api/v1/projects/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Project 'nope' not found"));
    }
}
