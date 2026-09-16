package com.pharmacy.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.common.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Path;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "app.messaging.enabled=false",
        "spring.cache.type=simple",
        "spring.modulith.events.republish-outstanding-events-on-restart=false"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class SecurityPrescriptionMySqlIntegrationTest {
    private static final byte[] PDF = "%PDF-1.4\nq1-security-test\n%%EOF\n".getBytes();
    private static Path storageRoot;

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void prescriptionStorage(DynamicPropertyRegistry registry) {
        storageRoot = tempDir;
        registry.add("app.prescription.storage-path", () -> storageRoot.toString());
    }

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    private String marker;
    private Long userId;
    private Long otherUserId;
    private Long pharmacistId;
    private String username;
    private String otherUsername;
    private String pharmacistUsername;

    @BeforeEach
    void seedUsers() {
        marker = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        username = "q1_user_" + marker;
        otherUsername = "q1_other_" + marker;
        pharmacistUsername = "q1_rx_" + marker;
        String hash = passwordEncoder.encode("Passw0rd!");
        jdbc.update("INSERT INTO sys_user(username,password,nickname,role,status) VALUES(?,?,?,?,1)",
                username, hash, "Q1用户", "USER");
        jdbc.update("INSERT INTO sys_user(username,password,nickname,role,status) VALUES(?,?,?,?,1)",
                otherUsername, hash, "Q1他人", "USER");
        jdbc.update("INSERT INTO sys_user(username,password,nickname,role,status) VALUES(?,?,?,?,1)",
                pharmacistUsername, hash, "Q1药师", "PHARMACIST");
        userId = jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, username);
        otherUserId = jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, otherUsername);
        pharmacistId = jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, pharmacistUsername);
        jdbc.update("INSERT INTO sys_user_role(user_id,role_id) SELECT ?, id FROM sys_role WHERE role_code=?", userId, "USER");
        jdbc.update("INSERT INTO sys_user_role(user_id,role_id) SELECT ?, id FROM sys_role WHERE role_code=?", otherUserId, "USER");
        jdbc.update("INSERT INTO sys_user_role(user_id,role_id) SELECT ?, id FROM sys_role WHERE role_code=?", pharmacistId, "PHARMACIST");
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM prescription_item WHERE prescription_id IN (SELECT id FROM prescription WHERE user_id IN (?,?,?))",
                userId, otherUserId, pharmacistId);
        jdbc.update("DELETE FROM prescription WHERE user_id IN (?,?,?)", userId, otherUserId, pharmacistId);
        jdbc.update("DELETE FROM refresh_token WHERE user_id IN (?,?,?)", userId, otherUserId, pharmacistId);
        jdbc.update("DELETE FROM sys_user_role WHERE user_id IN (?,?,?)", userId, otherUserId, pharmacistId);
        jdbc.update("DELETE FROM sys_user WHERE id IN (?,?,?)", userId, otherUserId, pharmacistId);
    }

    @Test
    void refreshRotatesCookieAndReplayRevokesFamily() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"Passw0rd!\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn();
        String firstRefresh = login.getResponse().getCookie("refresh_token").getValue();
        String firstAccess = objectMapper.readTree(login.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        MvcResult refreshed = mvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", firstRefresh)))
                .andExpect(status().isOk())
                .andReturn();
        String secondRefresh = refreshed.getResponse().getCookie("refresh_token").getValue();
        assertNotEquals(firstRefresh, secondRefresh);

        mvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", firstRefresh)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED))
                .andExpect(jsonPath("$.message", containsString("重放")));

        mvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", secondRefresh)))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + firstAccess))
                .andExpect(status().isOk());
    }

    @Test
    void logoutRejectsAccessTokenAfterwards() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"Passw0rd!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(login.getResponse().getContentAsString()).path("data");
        String access = data.path("accessToken").asText();
        String refresh = login.getResponse().getCookie("refresh_token").getValue();

        mvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + access)
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", refresh)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void userCannotAccessPharmacistApi() throws Exception {
        String access = loginAccess(username);
        mvc.perform(get("/api/pharmacist/prescriptions").header("Authorization", "Bearer " + access))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN));
    }

    @Test
    void nonOwnerPrescriptionDownloadReturnsNotFound() throws Exception {
        String ownerAccess = loginAccess(username);
        MockMultipartFile file = new MockMultipartFile("file", "rx.pdf", "application/pdf", PDF);
        MvcResult uploaded = mvc.perform(multipart("/api/user/prescriptions")
                        .file(file)
                        .param("medicineIds", "1")
                        .param("quantities", "1")
                        .header("Authorization", "Bearer " + ownerAccess))
                .andExpect(status().isOk())
                .andReturn();
        long prescriptionId = objectMapper.readTree(uploaded.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        assertTrue(prescriptionId > 0);

        String otherAccess = loginAccess(otherUsername);
        mvc.perform(get("/api/user/prescriptions/" + prescriptionId + "/file")
                        .header("Authorization", "Bearer " + otherAccess))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND))
                .andExpect(jsonPath("$.message").value("处方不存在"));
    }

    private String loginAccess(String name) throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + name + "\",\"password\":\"Passw0rd!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }
}
