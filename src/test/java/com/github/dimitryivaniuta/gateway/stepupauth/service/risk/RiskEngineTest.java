package com.github.dimitryivaniuta.gateway.stepupauth.service.risk;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for {@link RiskEngine}. */
class RiskEngineTest {

    @Test
    void shouldRequireStepUp_forHighAmountAndNewDevice() {
        RiskEngine engine = new RiskEngine();
        ReflectionTestUtils.setField(engine, "highAmountThreshold", new BigDecimal("1000.00"));
        ReflectionTestUtils.setField(engine, "stepUpThreshold", 70);
        ReflectionTestUtils.setField(engine, "newDeviceScore", 50);
        ReflectionTestUtils.setField(engine, "newCountryScore", 30);
        ReflectionTestUtils.setField(engine, "highAmountScore", 60);

        RiskAssessment a = engine.assess(true, false, new BigDecimal("5000.00"));
        assertThat(a.stepUpRequired()).isTrue();
        assertThat(a.level()).isEqualTo(RiskLevel.HIGH);
        assertThat(a.reasons()).contains("NEW_DEVICE").contains("HIGH_AMOUNT");
    }
}
