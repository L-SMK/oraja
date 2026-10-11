package bms.player.beatoraja.camera;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.bytedeco.javacpp.Loader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises the shipped executable and RGB pipe without opening a camera or a GUI. */
class FFmpegCameraNativeTest {
    @Test @Timeout(15) void bundledExecutableProducesRgbAndTerminatesAfterDemandStops() throws Exception {
        String executable = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
        AtomicReference<Process> process = new AtomicReference<>();
        try (CameraCapture capture = new CameraCapture(settings -> {
            Process child = FFmpegCameraProcess.start(new ProcessBuilder(executable, "-hide_banner", "-loglevel", "error", "-nostdin",
                    "-re", "-f", "lavfi", "-i", "color=c=red:s=160x120:r=10", "-an",
                    "-threads", "1", "-pix_fmt", "rgb24", "-f", "rawvideo", "pipe:1")
                    .redirectError(ProcessBuilder.Redirect.DISCARD));
            process.set(child);
            return new CameraCapture.Source() {
                public InputStream pixels() { return child.getInputStream(); }
                public void close() { FFmpegCameraProcess.stop(child); }
            };
        })) {
            capture.request(new HandCameraSettings(true, "synthetic", 160, 120, 10, false));
            CameraCapture.Frame frame = null;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (frame == null && System.nanoTime() < deadline) {
                frame = capture.poll();
                if (frame == null) Thread.sleep(10);
            }
            assertNotNull(frame, "No RGB frame from bundled FFmpeg");
            try (var received = frame) {
                byte[] pixels = received.pixels();
                assertEquals(160 * 120 * 3, pixels.length);
                assertTrue(Byte.toUnsignedInt(pixels[0]) > 240, "Red must stay red (no BGR swap)");
                assertTrue(Byte.toUnsignedInt(pixels[1]) < 10);
                assertTrue(Byte.toUnsignedInt(pixels[2]) < 10);
            }
            capture.stop();
            assertTrue(process.get().waitFor(2, TimeUnit.SECONDS), "Camera child must terminate");
            assertNull(capture.poll());
        } finally {
            if (process.get() != null) process.get().destroyForcibly();
        }
    }

    @Test @Timeout(15) void shutdownCleanupKillsAChildWithoutWaitingForDelayedReaping() throws Exception {
        String executable = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
        Process process = FFmpegCameraProcess.start(new ProcessBuilder(executable, "-hide_banner", "-loglevel", "error",
                "-nostdin", "-re", "-f", "lavfi", "-i", "color=c=red:s=160x120:r=10", "-f", "null", "-")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD));
        try {
            assertTrue(process.isAlive());
            FFmpegCameraProcess.terminateAll();
            assertTrue(process.waitFor(2, TimeUnit.SECONDS));
        } finally { FFmpegCameraProcess.stop(process); }
    }
}
