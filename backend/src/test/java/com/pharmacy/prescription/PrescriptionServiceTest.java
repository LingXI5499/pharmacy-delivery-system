package com.pharmacy.prescription;

import com.pharmacy.common.ErrorCode;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.mapper.OrderStatusLogMapper;
import com.pharmacy.mapper.PharmacyOrderItemMapper;
import com.pharmacy.mapper.PharmacyOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {
    private static final byte[] PDF = "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n".getBytes();
    private static final byte[] PNG = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53,
            (byte) 0xDE, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
            0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    @TempDir Path tempDir;
    @Mock private PrescriptionMapper mapper;
    @Mock private PrescriptionItemMapper itemMapper;
    @Mock private PharmacyOrderMapper orderMapper;
    @Mock private PharmacyOrderItemMapper orderItemMapper;
    @Mock private OrderStatusLogMapper logMapper;
    @Mock private InventoryService inventoryService;
    @Mock private ApplicationEventPublisher events;
    @InjectMocks private PrescriptionService service;

    @BeforeEach
    void setStorage() {
        ReflectionTestUtils.setField(service, "storagePath", tempDir.toString());
    }

    @AfterEach
    void assertNoLeakOutsideTemp() throws IOException {
        Path defaultStorage = Path.of("./data/prescriptions").toAbsolutePath().normalize();
        if (Files.isDirectory(defaultStorage)) {
            try (var walk = Files.walk(defaultStorage)) {
                assertFalse(walk.anyMatch(path -> path.getFileName().toString().endsWith(".pdf")
                        || path.getFileName().toString().endsWith(".png")
                        || path.getFileName().toString().endsWith(".jpg")),
                        "处方测试不得写入默认静态/默认存储目录");
            }
        }
    }

    @Test
    void uploadRejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "rx.pdf", "application/pdf", new byte[0]);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.upload(1L, empty, List.of(10L), List.of(1)));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getCode());
    }

    @Test
    void uploadRejectsOversizedFile() {
        MultipartFile oversized = mock(MultipartFile.class);
        when(oversized.isEmpty()).thenReturn(false);
        when(oversized.getSize()).thenReturn(10L * 1024 * 1024 + 1);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.upload(1L, oversized, List.of(10L), List.of(1)));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getCode());
    }

    @Test
    void uploadRejectsForgedPdfExtensionWithPlainText() {
        MockMultipartFile forged = new MockMultipartFile(
                "file", "legit.pdf", "application/pdf", "not-a-real-pdf".getBytes());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.upload(1L, forged, List.of(10L), List.of(1)));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getCode());
        assertTrue(ex.getMessage().contains("PDF"));
    }

    @Test
    void uploadRejectsWrongMimeContent() {
        MockMultipartFile wrong = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "hello world".getBytes());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.upload(1L, wrong, List.of(10L), List.of(1)));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getCode());
    }

    @Test
    void uploadAcceptsRealPdfDespiteSuspiciousExecutableName() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../evil.exe.pdf", "application/octet-stream", PDF);
        when(mapper.insert(any(Prescription.class))).thenAnswer(invocation -> {
            Prescription p = invocation.getArgument(0);
            p.setId(100L);
            return 1;
        });
        Prescription saved = withTransaction(() -> service.upload(9L, file, List.of(11L), List.of(2)));
        assertEquals(100L, saved.getId());
        assertEquals("application/pdf", saved.getContentType());
        assertEquals("evil.exe.pdf", saved.getOriginalFilename());
        assertTrue(Files.isRegularFile(tempDir.resolve(saved.getStorageKey())));
        assertTrue(saved.getStorageKey().endsWith(".pdf"));
        assertFalse(saved.getStorageKey().contains(".."));
        verify(itemMapper).insert(any(PrescriptionItem.class));
    }

    @Test
    void uploadAcceptsRealPng() {
        MockMultipartFile file = new MockMultipartFile("file", "shot.png", "image/png", PNG);
        when(mapper.insert(any(Prescription.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Prescription.class).setId(101L);
            return 1;
        });
        Prescription saved = withTransaction(() -> service.upload(2L, file, List.of(1L), List.of(1)));
        assertEquals("image/png", saved.getContentType());
        assertTrue(Files.isRegularFile(tempDir.resolve(saved.getStorageKey())));
    }

    @Test
    void getAuthorizedHidesExistenceFromNonOwner() {
        Prescription owned = new Prescription();
        owned.setId(5L);
        owned.setUserId(1L);
        when(mapper.selectById(5L)).thenReturn(owned);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getAuthorized(5L, 99L, false));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
        assertEquals("处方不存在", ex.getMessage());
    }

    @Test
    void getAuthorizedAllowsOwnerAndPrivileged() {
        Prescription owned = new Prescription();
        owned.setId(5L);
        owned.setUserId(1L);
        when(mapper.selectById(5L)).thenReturn(owned);
        assertEquals(owned, service.getAuthorized(5L, 1L, false));
        assertEquals(owned, service.getAuthorized(5L, 99L, true));
    }

    @Test
    void resourceRejectsPathTraversalStorageKey() throws Exception {
        Path outside = Files.createTempFile("outside-rx-", ".pdf");
        Files.write(outside, PDF);
        try {
            Prescription p = new Prescription();
            p.setStorageKey("../" + outside.getFileName());
            BusinessException ex = assertThrows(BusinessException.class, () -> service.resource(p));
            assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void uploadPersistsOnlyUnderConfiguredTempRoot() {
        MockMultipartFile file = new MockMultipartFile("file", "rx.pdf", "application/pdf", PDF);
        when(mapper.insert(any(Prescription.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Prescription.class).setId(1L);
            return 1;
        });
        Prescription saved = withTransaction(() -> service.upload(1L, file, List.of(1L), List.of(1)));
        Path stored = tempDir.resolve(saved.getStorageKey()).normalize();
        assertTrue(stored.startsWith(tempDir.toAbsolutePath().normalize()));
        ArgumentCaptor<Prescription> captor = ArgumentCaptor.forClass(Prescription.class);
        verify(mapper).insert(captor.capture());
        assertEquals(saved.getStorageKey(), captor.getValue().getStorageKey());
    }

    private static <T> T withTransaction(Supplier<T> action) {
        TransactionSynchronizationManager.initSynchronization();
        try {
            return action.get();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
