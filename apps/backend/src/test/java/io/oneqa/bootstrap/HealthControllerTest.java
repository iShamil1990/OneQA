package io.oneqa.bootstrap;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthControllerTest {
    @Test
    void pingReturnsUp() {
        var result = new HealthController().ping();
        assertEquals("UP", result.get("status"));
        assertEquals("OneQA", result.get("application"));
    }
}
