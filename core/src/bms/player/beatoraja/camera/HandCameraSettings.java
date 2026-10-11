package bms.player.beatoraja.camera;

import bms.player.beatoraja.Config;

/** Capture limits are enforced even when configuration was edited by hand. */
public record HandCameraSettings(boolean enabled, String device, int width, int height, int fps,
        boolean mirror) {
    public HandCameraSettings {
        device = device == null ? "" : device.trim();
        width = Math.max(160, Math.min(1920, width));
        height = Math.max(120, Math.min(1080, height));
        fps = Math.max(1, Math.min(60, fps));
    }

    public static HandCameraSettings from(Config config) {
        return new HandCameraSettings(config.isHandCameraEnabled(), config.getHandCameraDevice(),
                config.getHandCameraWidth(), config.getHandCameraHeight(), config.getHandCameraFps(),
                config.isHandCameraMirror());
    }

    public boolean usable() { return enabled && !device.isEmpty(); }
    public int frameBytes() { return width * height * 3; }
}
