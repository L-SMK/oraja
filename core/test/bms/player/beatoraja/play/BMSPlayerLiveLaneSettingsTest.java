package bms.player.beatoraja.play;

import bms.player.beatoraja.PlayConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BMSPlayerLiveLaneSettingsTest {
    @Test
    void floatingRuntimePersistsMultiplierAndUnroundedGreenWithoutReplacingLegacySpeed() {
        PlayConfig saved = new PlayConfig(); saved.setBmsirFloatingHispeedEnabled(true);
        saved.setHispeed(4); saved.setDuration(650);
        PlayConfig live = saved.clone(); live.setBmsirFloatingHispeed(3.125f);
        live.setBmsirFloatingGreenFrames(32.123456f); live.setBmsirFloatingCoverVisible(false);
        BMSPlayer.copyLiveLaneSettings(saved, live, false);
        assertEquals(3.125f, saved.getBmsirFloatingHispeed());
        assertEquals(32.123456f, saved.getBmsirFloatingGreenFrames());
        assertEquals(4f, saved.getHispeed()); assertEquals(650, saved.getDuration());
        assertTrue(saved.isBmsirFloatingCoverVisible());
    }
    @Test
    void fixedHispeedPersistsTheLiveDurationAndCompleteCoverState() {
        PlayConfig saved = new PlayConfig();
        saved.setFixhispeed(PlayConfig.FIX_HISPEED_MAINBPM);
        saved.setHispeed(1.0f);
        saved.setDuration(500);

        PlayConfig live = new PlayConfig();
        live.setFixhispeed(PlayConfig.FIX_HISPEED_MAINBPM);
        live.setHispeed(2.5f);
        live.setDuration(384);
        live.setLanecover(0.42f);
        live.setEnablelanecover(false);
        live.setLift(0.18f);
        live.setEnablelift(true);
        live.setHidden(0.27f);
        live.setEnablehidden(true);

        BMSPlayer.copyLiveLaneSettings(saved, live);

        assertEquals(384, saved.getDuration());
        assertEquals(1.0f, saved.getHispeed());
        assertEquals(0.42f, saved.getLanecover());
        assertFalse(saved.isEnablelanecover());
        assertEquals(0.18f, saved.getLift());
        assertTrue(saved.isEnablelift());
        assertEquals(0.27f, saved.getHidden());
        assertTrue(saved.isEnablehidden());
    }

    @Test
    void unfixedHispeedPersistsTheLiveMultiplierWithoutReplacingDuration() {
        PlayConfig saved = new PlayConfig();
        saved.setFixhispeed(PlayConfig.FIX_HISPEED_OFF);
        saved.setHispeed(1.0f);
        saved.setDuration(500);

        PlayConfig live = new PlayConfig();
        live.setFixhispeed(PlayConfig.FIX_HISPEED_OFF);
        live.setHispeed(3.25f);
        live.setDuration(275);

        BMSPlayer.copyLiveLaneSettings(saved, live);

        assertEquals(3.25f, saved.getHispeed());
        assertEquals(500, saved.getDuration());
    }

    @Test
    void lr2OverridePersistsStoredHispeedDurationAndBaseScrollTogether() {
        PlayConfig saved = new PlayConfig();
        saved.setFixhispeed(PlayConfig.FIX_HISPEED_MAINBPM);

        PlayConfig live = new PlayConfig();
        live.setHispeed(2.75f);
        live.setDuration(431);
        live.setBmsirBaseScrollSpeed(137);
        live.setBmsirHispeedReferenceBpm(173);

        BMSPlayer.copyLiveLaneSettings(saved, live, true);

        assertEquals(2.75f, saved.getHispeed());
        assertEquals(431, saved.getDuration());
        assertEquals(137, saved.getBmsirBaseScrollSpeed());
        assertEquals(173, saved.getBmsirHispeedReferenceBpm());
    }
}
