package bms.player.beatoraja.audio;

import bms.player.beatoraja.*;
import bms.player.beatoraja.play.BMSPlayer;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

class LoudnessVolumeTest {
    private static void set(Object target, Class<?> owner, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    @Test void gainRespectsMuteRatioAndFailureWithoutAmplifyingInvalidLoudness() {
        var result = new BMSLoudnessAnalyzer.AnalysisResult(null, -18);
        assertEquals(0, result.calculateAdjustedVolume(0));
        assertEquals(0.4988f, result.calculateAdjustedVolume(0.25f), 0.001f);
        assertEquals(0.1995f, result.calculateAdjustedVolume(0.1f), 0.001f);
        assertEquals(1, result.calculateAdjustedVolume(1));
        assertEquals(0.3f, new BMSLoudnessAnalyzer.AnalysisResult(null, "failure").calculateAdjustedVolume(0.3f));
        assertEquals(0.3f, new BMSLoudnessAnalyzer.AnalysisResult(null, Double.NEGATIVE_INFINITY).calculateAdjustedVolume(0.3f));
    }
    @Test void liveKeyAndBackgroundSettingsAreAppliedIndependently() throws Exception {
        Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe)f.get(null);
        MainController main = (MainController)unsafe.allocateInstance(MainController.class);
        BMSPlayer player = (BMSPlayer)unsafe.allocateInstance(BMSPlayer.class);
        Config config = new Config(); config.setAudioConfig(new AudioConfig());
        config.getAudioConfig().setKeyvolume(0); config.getAudioConfig().setBgvolume(0.2f);
        set(main, MainController.class, "config", config); set(player, MainState.class, "main", main);
        set(player, BMSPlayer.class, "normalizationResult", new BMSLoudnessAnalyzer.AnalysisResult(null, -12));
        assertEquals(0, player.getAdjustedVolume()); assertEquals(0.2f, player.getAdjustedBackgroundVolume());
        config.getAudioConfig().setKeyvolume(0.7f); config.getAudioConfig().setBgvolume(0);
        assertEquals(0.7f, player.getAdjustedVolume()); assertEquals(0, player.getAdjustedBackgroundVolume());
    }
}
