package com.pharmacy;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTest {
    @Test
    void moduleDependenciesMustNotContainCycles() {
        ApplicationModules.of(PharmacyDeliveryApplication.class).verify();
    }
}
