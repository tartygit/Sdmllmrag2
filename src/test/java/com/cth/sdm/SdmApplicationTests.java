package com.cth.sdm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SdmApplicationTests {

    @Test
    void contextLoads() {
        // Simple verification that Spring context bootstraps correctly
    }
}
