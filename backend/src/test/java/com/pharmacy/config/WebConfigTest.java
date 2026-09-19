package com.pharmacy.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class WebConfigTest {

    @Test
    void parseAllowedOriginsSplitsCommaSeparatedLocalDevPorts() {
        assertArrayEquals(
                new String[] {"http://localhost:5173", "http://localhost:5174"},
                WebConfig.parseAllowedOrigins("http://localhost:5173,http://localhost:5174"));
    }

    @Test
    void parseAllowedOriginsTrimsBlanks() {
        assertArrayEquals(
                new String[] {"http://localhost:5173", "http://127.0.0.1:4173"},
                WebConfig.parseAllowedOrigins(" http://localhost:5173 , http://127.0.0.1:4173 "));
    }
}
