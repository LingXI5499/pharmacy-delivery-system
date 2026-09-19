package com.pharmacy.prescription;

import com.pharmacy.common.ErrorCode;
import com.pharmacy.enums.UserRole;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.exception.GlobalExceptionHandler;
import com.pharmacy.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PrescriptionControllerTest {
    private PrescriptionService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(PrescriptionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PrescriptionController(service))
                .setControllerAdvice(new GlobalExceptionHandler(mock(com.pharmacy.observability.PharmacyBusinessMetrics.class)))
                .build();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nonOwnerDownloadReturnsNotFoundWithoutLeakingExistence() throws Exception {
        authenticate(2L, "other", UserRole.USER);
        when(service.getAuthorized(eq(55L), eq(2L), eq(false)))
                .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "处方不存在"));

        mvc.perform(get("/api/user/prescriptions/55/file"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND))
                .andExpect(jsonPath("$.message").value("处方不存在"));
    }

    @Test
    void ownerCanDownloadOwnPrescriptionFile() throws Exception {
        authenticate(1L, "owner", UserRole.USER);
        Prescription prescription = new Prescription();
        prescription.setId(55L);
        prescription.setUserId(1L);
        prescription.setContentType("application/pdf");
        when(service.getAuthorized(55L, 1L, false)).thenReturn(prescription);
        when(service.resource(prescription)).thenReturn(new ByteArrayResource("%PDF".getBytes()));

        mvc.perform(get("/api/user/prescriptions/55/file"))
                .andExpect(status().isOk());
        verify(service).getAuthorized(55L, 1L, false);
    }

    @Test
    void pharmacistCanDownloadAsPrivileged() throws Exception {
        authenticate(9L, "rx", UserRole.PHARMACIST);
        Prescription prescription = new Prescription();
        prescription.setId(55L);
        prescription.setUserId(1L);
        prescription.setContentType("image/png");
        when(service.getAuthorized(eq(55L), eq(9L), anyBoolean())).thenReturn(prescription);
        when(service.resource(prescription)).thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}));

        mvc.perform(get("/api/pharmacist/prescriptions/55/file"))
                .andExpect(status().isOk());
        verify(service).getAuthorized(55L, 9L, true);
    }

    private void authenticate(Long id, String username, UserRole role) {
        AuthenticatedUser user = new AuthenticatedUser(id, username, username, role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }
}
