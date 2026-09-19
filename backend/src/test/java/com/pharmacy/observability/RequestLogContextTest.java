package com.pharmacy.observability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestLogContextTest {
    @Test
    void sanitizeRedactsTokensAndPrescriptionPaths() {
        assertEquals("[redacted]", RequestLogContext.sanitize("Bearer abc.def"));
        assertEquals("[redacted]", RequestLogContext.sanitize("refresh_token=xyz"));
        assertEquals("[redacted]", RequestLogContext.sanitize("/data/prescriptions/a.pdf"));
        assertEquals("ORDER-1", RequestLogContext.sanitize("ORDER-1"));
        assertNull(RequestLogContext.sanitize(" "));
    }
}
