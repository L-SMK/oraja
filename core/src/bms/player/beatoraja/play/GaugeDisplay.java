package bms.player.beatoraja.play;

/** Numeric skin display for ordinary floating-point gauges. */
public final class GaugeDisplay {
    private GaugeDisplay() {}

    public static int integerPart(float value) {
        return value > 0f && value < 1f ? 1 : (int) value;
    }

    public static int fractionalPart(float value) {
        return value > 0f && value < 1f ? 0 : ((int) (value * 10)) % 10;
    }
}
