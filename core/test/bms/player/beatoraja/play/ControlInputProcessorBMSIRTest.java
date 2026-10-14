package bms.player.beatoraja.play;

import bms.model.Mode;
import bms.player.beatoraja.PlayerConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlInputProcessorBMSIRTest {
    @Test
    void abortChordThenSelectRetryCannotToggleHidden() {
        var taps = new ControlInputProcessor.SelectDoublePress(false);
        assertFalse(taps.update(true, true, 1000)); // SELECT arrives before START.
        assertFalse(taps.update(true, false, 1010)); // START + SELECT exit chord.
        assertFalse(taps.update(false, false, 1100)); // Exit consumes both inputs.
        assertFalse(taps.update(true,
                ControlInputProcessor.isLaneControlActive(BMSPlayer.STATE_ABORTED), 1200));

        // A new play inherits the held SELECT used to request the retry.
        var retry = new ControlInputProcessor.SelectDoublePress(true);
        assertFalse(retry.update(true, true, 1210));
        assertFalse(retry.update(false, true, 1220));
        assertFalse(retry.update(true, true, 1230));
        assertFalse(retry.update(false, true, 1240));
        assertTrue(retry.update(true, true, 1250)); // Two fresh, intentional taps still work.
    }

    @Test
    void exitChordClearsPendingTapAndDoesNotArmOnSelectOnlyReleaseTail() {
        var taps = new ControlInputProcessor.SelectDoublePress(false);
        assertFalse(taps.update(true, true, 1000));
        assertFalse(taps.update(false, true, 1010));
        assertFalse(taps.update(true, false, 1100));
        assertFalse(taps.update(true, true, 1110)); // START released before SELECT.
        assertFalse(taps.update(false, true, 1120));
        assertFalse(taps.update(true, true, 1200));
        assertFalse(taps.update(false, true, 1210));
        assertTrue(taps.update(true, true, 1300));
    }

    @Test
    void hiddenDoublePressRequiresFreshEdgesWithinFiveHundredMilliseconds() {
        var taps = new ControlInputProcessor.SelectDoublePress(false);
        assertFalse(taps.update(true, true, 0));
        assertFalse(taps.update(true, true, 100)); // Holding is not a second press.
        assertFalse(taps.update(false, true, 200));
        assertTrue(taps.update(true, true, 499));
        assertFalse(taps.update(false, true, 500));
        assertFalse(taps.update(true, true, 600)); // Each toggle consumes its pair.
        assertFalse(taps.update(false, true, 610));
        assertFalse(taps.update(true, true, 1100)); // Exactly 500 ms is too late.
        assertFalse(taps.update(false, true, 1110));
        assertTrue(taps.update(true, true, 1200));
    }

    @Test
    void closingStatesBlockLaneChangesAndClearPendingHiddenTaps() {
        for (int state : new int[]{BMSPlayer.STATE_FAILED, BMSPlayer.STATE_FINISHED,
                BMSPlayer.STATE_ABORTED, BMSPlayer.STATE_PRACTICE_FINISHED}) {
            assertFalse(ControlInputProcessor.isLaneControlActive(state));
            var taps = new ControlInputProcessor.SelectDoublePress(false);
            assertFalse(taps.update(true, true, 1000));
            assertFalse(taps.update(false, ControlInputProcessor.isLaneControlActive(state), 1010));
            assertFalse(taps.update(true, true, 1100));
        }
        for (int state : new int[]{BMSPlayer.STATE_PRELOAD, BMSPlayer.STATE_READY,
                BMSPlayer.STATE_PLAY, BMSPlayer.STATE_WAIT, BMSPlayer.STATE_PRACTICE}) {
            assertTrue(ControlInputProcessor.isLaneControlActive(state));
        }
    }

    @Test
    void sixAndSevenKeysAreMappedForSevenAndFourteenKeyModesOnly() {
        assertTrue(ControlInputProcessor.isSixSevenKey(Mode.BEAT_7K, 5));
        assertTrue(ControlInputProcessor.isSixSevenKey(Mode.BEAT_7K, 6));
        assertTrue(ControlInputProcessor.isSixSevenKey(Mode.BEAT_14K, 14));
        assertTrue(ControlInputProcessor.isSixSevenKey(Mode.BEAT_14K, 15));
        assertFalse(ControlInputProcessor.isSixSevenKey(Mode.BEAT_5K, 5));
        assertFalse(ControlInputProcessor.isSixSevenKey(Mode.POPN_9K, 6));
    }

    @Test
    void lr2RequiresSuddenWhileExtendedAcceptsAnyCoverTarget() {
        assertFalse(ControlInputProcessor.usesSixSevenCoverControl(
                PlayerConfig.BMSIR_COVER_CONTROL_ORAJA,
                true,
                true,
                true
        ));
        assertTrue(ControlInputProcessor.usesSixSevenCoverControl(
                PlayerConfig.BMSIR_COVER_CONTROL_LR2,
                true,
                false,
                false
        ));
        assertFalse(ControlInputProcessor.usesSixSevenCoverControl(
                PlayerConfig.BMSIR_COVER_CONTROL_LR2,
                false,
                true,
                true
        ));
        assertTrue(ControlInputProcessor.usesSixSevenCoverControl(
                PlayerConfig.BMSIR_COVER_CONTROL_EXTENDED,
                false,
                true,
                false
        ));
        assertTrue(ControlInputProcessor.usesSixSevenCoverControl(
                PlayerConfig.BMSIR_COVER_CONTROL_EXTENDED,
                false,
                false,
                true
        ));
        assertFalse(ControlInputProcessor.usesSixSevenCoverControl(
                PlayerConfig.BMSIR_COVER_CONTROL_EXTENDED,
                false,
                false,
                false
        ));
    }

    @Test
    void keySevenIsTheIncreasingDirectionOnBothPlayers() {
        assertFalse(ControlInputProcessor.isSevenKey(Mode.BEAT_7K, 5));
        assertTrue(ControlInputProcessor.isSevenKey(Mode.BEAT_7K, 6));
        assertFalse(ControlInputProcessor.isSevenKey(Mode.BEAT_14K, 14));
        assertTrue(ControlInputProcessor.isSevenKey(Mode.BEAT_14K, 15));
    }

    @Test
    void sixSevenFhsRecalculationRequiresBothSwitchesAndCurrentBpm() {
        assertFalse(ControlInputProcessor.shouldRecalculateSixSevenHispeed(
                false, false, 150.0
        ));
        assertFalse(ControlInputProcessor.shouldRecalculateSixSevenHispeed(
                true, false, 150.0
        ));
        assertFalse(ControlInputProcessor.shouldRecalculateSixSevenHispeed(
                false, true, 150.0
        ));
        assertFalse(ControlInputProcessor.shouldRecalculateSixSevenHispeed(
                true, true, 0.0
        ));
        assertTrue(ControlInputProcessor.shouldRecalculateSixSevenHispeed(
                true, true, 150.0
        ));
    }

    @Test
    void ordinaryCoverRecalculationRequiresItsSwitchAndUsableCurrentBpm() {
        assertFalse(ControlInputProcessor.shouldRecalculateCoverHispeed(
                false, 150.0
        ));
        assertFalse(ControlInputProcessor.shouldRecalculateCoverHispeed(
                true, 0.0
        ));
        assertFalse(ControlInputProcessor.shouldRecalculateCoverHispeed(
                true, Double.NaN
        ));
        assertTrue(ControlInputProcessor.shouldRecalculateCoverHispeed(
                true, 150.0
        ));
    }

	@Test
	void floatingHispeedRequiresManualSpeedAndEveryCoverOff() {
		assertTrue(ControlInputProcessor.usesFloatingHispeed(false, false, false, false));
		assertFalse(ControlInputProcessor.usesFloatingHispeed(true, false, false, false));
		assertFalse(ControlInputProcessor.usesFloatingHispeed(false, true, false, false));
		assertFalse(ControlInputProcessor.usesFloatingHispeed(false, false, true, false));
		assertFalse(ControlInputProcessor.usesFloatingHispeed(false, false, false, true));
	}

}
