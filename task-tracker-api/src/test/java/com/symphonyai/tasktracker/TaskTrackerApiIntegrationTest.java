package com.symphonyai.tasktracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskTrackerApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private long createProject(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"description\":\"desc\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private long createUser(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ada\",\"email\":\"" + email + "\",\"role\":\"DEVELOPER\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    @Test
    void infoEndpoint_exposesConfiguredNameAndVersion() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Task Tracker API")))
                .andExpect(jsonPath("$.version", is("1.0.0")));
    }

    @Test
    void projectCrud_andTaskLifecycle() throws Exception {
        long projectId = createProject("Website Revamp");
        long userId = createUser("ada@example.com");

        // duplicate project name -> 409
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Website Revamp\"}"))
                .andExpect(status().isConflict());

        // create task with default priority (MEDIUM from config)
        MvcResult taskResult = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Landing page\",\"projectId\":" + projectId
                                + ",\"assigneeId\":" + userId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("TODO")))
                .andExpect(jsonPath("$.priority", is("MEDIUM")))
                .andExpect(jsonPath("$.projectId", is((int) projectId)))
                .andExpect(jsonPath("$.assigneeId", is((int) userId)))
                .andReturn();
        long taskId = objectMapper.readTree(taskResult.getResponse().getContentAsString())
                .get("id").asLong();

        // tasks under project
        mockMvc.perform(get("/api/projects/{id}/tasks", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements", is(1)));

        // update task status
        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Landing page\",\"status\":\"DONE\","
                                + "\"priority\":\"HIGH\",\"projectId\":" + projectId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DONE")));

        // filter tasks by status
        mockMvc.perform(get("/api/tasks").param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        // delete task -> subsequent get returns 404
        mockMvc.perform(delete("/api/tasks/{id}", taskId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/tasks/{id}", taskId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void validationErrors_returnStructured400() throws Exception {
        // missing title + missing projectId
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.projectId").exists());

        // invalid email
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bob\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());

        // invalid enum value
        long projectId = createProject("Enum Project");
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"status\":\"NOPE\",\"projectId\":"
                                + projectId + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void relationalConstraints_areEnforced() throws Exception {
        // task against a non-existent project -> 404
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Orphan\",\"projectId\":9999}"))
                .andExpect(status().isNotFound());

        // deleting a user unassigns their tasks (no FK violation)
        long projectId = createProject("Constraint Project");
        long userId = createUser("owner@example.com");
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Assigned\",\"projectId\":" + projectId
                                + ",\"assigneeId\":" + userId + "}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{id}/tasks", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].assigneeId").doesNotExist());
    }
}
