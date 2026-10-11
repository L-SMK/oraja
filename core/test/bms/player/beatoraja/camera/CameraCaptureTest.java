package bms.player.beatoraja.camera;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CameraCaptureTest {
    private final HandCameraSettings settings = new HandCameraSettings(true, "camera", 160, 120, 30, false);
    static final class Pipe implements CameraCapture.Source {
        final PipedInputStream input = new PipedInputStream(160 * 120 * 3 * 2);
        final PipedOutputStream output;
        final AtomicBoolean closed = new AtomicBoolean();
        Pipe() throws IOException { output = new PipedOutputStream(input); }
        public InputStream pixels() { return input; }
        public void close() {
            closed.set(true);
            try { output.close(); } catch (IOException ignored) { }
        }
    }
    static void until(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(5);
        assertTrue(condition.getAsBoolean());
    }
    @Test void disabledAndMissingDeviceNeverOpenTheBackend() throws Exception {
        AtomicInteger opens = new AtomicInteger();
        try (CameraCapture capture = new CameraCapture(s -> { opens.incrementAndGet(); return new Pipe(); })) {
            capture.request(new HandCameraSettings(false, "camera", 160, 120, 30, false));
            capture.request(new HandCameraSettings(true, "", 160, 120, 30, false));
            assertNull(capture.poll());
            assertEquals(0, opens.get());
        }
    }
    @Test void publishesOnlyCompleteFramesDropsOldFramesAndStopsBlockedReads() throws Exception {
        Pipe pipe = new Pipe();
        AtomicInteger readFrames = new AtomicInteger();
        CameraCapture.Source source = new CameraCapture.Source() {
            public InputStream pixels() { return pipe.input; }
            public void frameRead() { readFrames.incrementAndGet(); }
            public void close() { pipe.close(); }
        };
        try (CameraCapture capture = new CameraCapture(s -> source)) {
            capture.request(settings);
            pipe.output.write(new byte[100]);
            assertNull(capture.poll());
            pipe.output.write(new byte[settings.frameBytes() - 100]);
            until(() -> readFrames.get() >= 1);
            byte[] next = new byte[settings.frameBytes()];
            Arrays.fill(next, (byte) 7);
            pipe.output.write(next);
            until(() -> readFrames.get() >= 2);
            try (var frame = capture.poll()) {
                assertNotNull(frame);
                assertArrayEquals(next, frame.pixels());
                assertEquals(160, frame.width());
                assertEquals(120, frame.height());
            }
            capture.stop();
            assertTrue(pipe.closed.get());
            assertNull(capture.poll());
        }
    }
    @Test void stopDuringStartupClosesLateBackendAndDoesNotPublishOrReopen() throws Exception {
        CountDownLatch opening = new CountDownLatch(1), finish = new CountDownLatch(1);
        Pipe pipe = new Pipe();
        try (CameraCapture capture = new CameraCapture(s -> {
            opening.countDown();
            assertTrue(finish.await(3, TimeUnit.SECONDS));
            return pipe;
        })) {
            capture.request(settings);
            assertTrue(opening.await(3, TimeUnit.SECONDS));
            capture.stop();
            finish.countDown();
            until(pipe.closed::get);
            assertNull(capture.poll());
        } finally { finish.countDown(); }
    }
    @Test void partialEofFailsOncePerDemandPeriodAndReentryCanRetry() throws Exception {
        AtomicInteger opens = new AtomicInteger();
        try (CameraCapture capture = new CameraCapture(s -> {
            opens.incrementAndGet();
            return new CameraCapture.Source() {
                public InputStream pixels() { return new ByteArrayInputStream(new byte[5]); }
                public void close() { }
            };
        })) {
            capture.request(settings);
            until(() -> opens.get() == 1);
            // The same settings remain latched even if a device rejected them.
            for (int i = 0; i < 100; i++) capture.request(settings);
            assertEquals(1, opens.get());
            assertNull(capture.poll());
            capture.stop();
            capture.request(settings);
            until(() -> opens.get() == 2);
        }
    }
    @Test void aScreenWithNoCameraDemandClosesTheDeviceAndTheNextScreenCanReopen() throws Exception {
        var config = new bms.player.beatoraja.Config();
        config.setHandCameraEnabled(true);
        config.setHandCameraDevice("camera");
        AtomicInteger opens = new AtomicInteger();
        AtomicReference<Pipe> latest = new AtomicReference<>();
        CameraCapture capture = new CameraCapture(s -> {
            Pipe pipe = new Pipe(); latest.set(pipe); opens.incrementAndGet(); return pipe;
        });
        try (HandCameraManager manager = new HandCameraManager(config, capture)) {
            manager.beginFrame(); manager.endFrame();
            assertEquals(0, opens.get());
            manager.beginFrame(); assertNull(manager.requestTexture()); manager.endFrame();
            until(() -> opens.get() == 1);
            Pipe first = latest.get();
            manager.beginFrame(); manager.endFrame();
            assertTrue(first.closed.get());
            manager.beginFrame(); assertNull(manager.requestTexture()); manager.endFrame();
            until(() -> opens.get() == 2);
        }
        assertTrue(latest.get().closed.get());
    }
}
