package bms.player.beatoraja.play;

import bms.model.BMSModel;
import bms.model.Mode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GaugeDisplayTest {
    @Test
    void positiveSubPercentValuesDisplayOneWithNoFraction() {
        for (float value : new float[]{Float.MIN_VALUE, 0.01f, 0.09f, 0.1f, 0.49f,
                0.5f, 0.99f, Math.nextDown(1f), 1f}) {
            assertEquals(1, GaugeDisplay.integerPart(value), "integer at " + value);
            assertEquals(0, GaugeDisplay.fractionalPart(value), "fraction at " + value);
        }
    }

    @Test
    void zeroAndOrdinaryValuesKeepTheirExistingDigits() {
        for (float value : new float[]{0f, 1.1f, 1.9f, 2f, 24.66f, 79.98f, 100f}) {
            assertEquals((int) value, GaugeDisplay.integerPart(value));
            assertEquals(((int) (value * 10)) % 10, GaugeDisplay.fractionalPart(value));
        }
    }

    @Test
    void danGaugeStaysAliveAtHalfPercentAndDiesAtActualZero() {
        for (int type : new int[]{GrooveGauge.CLASS, GrooveGauge.EXCLASS, GrooveGauge.EXHARDCLASS}) {
            GrooveGauge gauge = new GrooveGauge(chart(), type, GaugeProperty.SEVENKEYS);
            gauge.setValue(type, 0.5f);
            assertEquals(1, gauge.getGauge().getDisplayValue());
            assertEquals(0.5f, gauge.getValue());
            assertTrue(gauge.isQualified());
            gauge.update(4);
            assertEquals(0f, gauge.getValue());
            assertEquals(0, gauge.getGauge().getDisplayValue());
            assertFalse(gauge.isQualified());
            gauge.update(0);
            assertEquals(0f, gauge.getValue());
        }
    }

    @Test
    void ordinaryGaugeTypesUseTheMinimumWhileNantokaKeepsTwoPercentSteps() {
        for (int type = GrooveGauge.ASSISTEASY; type <= GrooveGauge.EXHARDCLASS; type++) {
            GrooveGauge gauge = new GrooveGauge(chart(), type, GaugeProperty.SEVENKEYS);
            gauge.setValue(type, 0.5f);
            assertEquals(GaugeDisplay.integerPart(gauge.getValue()), gauge.getGauge().getDisplayValue());
        }
        GrooveGauge nantoka = new GrooveGauge(chart(), GrooveGauge.CLASS, GaugeProperty.NANTOKA_MANIA);
        nantoka.setValue(GrooveGauge.CLASS, 1.5f);
        assertEquals(1.5f, nantoka.getValue());
        assertEquals(0, nantoka.getGauge().getDisplayValue());
        nantoka.setValue(GrooveGauge.CLASS, 3.5f);
        assertEquals(2, nantoka.getGauge().getDisplayValue());
    }

    private static BMSModel chart() {
        BMSModel model = new BMSModel();
        model.setMode(Mode.BEAT_7K);
        model.setTotal(200);
        return model;
    }
}
