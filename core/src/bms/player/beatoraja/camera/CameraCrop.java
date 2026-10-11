package bms.player.beatoraja.camera;

/** Normalized top-left crop, independent of the capture resolution. */
public record CameraCrop(float x, float y, float width, float height) {
    public boolean valid() {
        return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(width) && Float.isFinite(height)
                && x >= 0 && y >= 0 && width > 0 && height > 0 && x + width <= 1 && y + height <= 1;
    }
    public int[] pixels(int frameWidth, int frameHeight) {
        if (!valid() || frameWidth < 1 || frameHeight < 1) throw new IllegalArgumentException("Invalid camera crop");
        int left = Math.min(frameWidth - 1, (int) (x * frameWidth));
        int top = Math.min(frameHeight - 1, (int) (y * frameHeight));
        int right = Math.min(frameWidth, Math.max(left + 1, Math.round((x + width) * frameWidth)));
        int bottom = Math.min(frameHeight, Math.max(top + 1, Math.round((y + height) * frameHeight)));
        return new int[] {left, top, right - left, bottom - top};
    }
}
