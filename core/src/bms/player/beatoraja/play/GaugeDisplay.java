package bms.player.beatoraja.play;

/** Keep an integer skin display from showing zero while gauge remains. */
public final class GaugeDisplay {
    private GaugeDisplay() {}

    public static int minimumPositivePercent(float value, int displayValue) {
        return value > 0f ? Math.max(1, displayValue) : displayValue;
    }
}
