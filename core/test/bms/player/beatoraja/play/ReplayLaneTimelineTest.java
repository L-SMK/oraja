package bms.player.beatoraja.play;

import bms.player.beatoraja.*;
import bms.player.beatoraja.input.KeyInputLog;
import com.badlogic.gdx.utils.Json;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReplayLaneTimelineTest {
    private static ReplayLaneState state(long time, float hs) {
        PlayConfig config = new PlayConfig();
        config.setHispeed(hs);
        return ReplayLaneState.capture(time, config, false);
    }

    @Test void recordsOnlyChangesAndCoalescesSameChartTime() {
        ReplayLaneTimeline timeline = new ReplayLaneTimeline(state(0, 1), null);
        timeline.record(state(10, 1));
        timeline.record(state(20, 2));
        timeline.record(state(20, 3));
        timeline.record(state(19, 4));
        assertEquals(1, timeline.recorded().length);
        assertEquals(3, timeline.recorded()[0].config.getHispeed());
        assertEquals(20, timeline.recorded()[0].time);
    }

    @Test void appliesAtChartTimeAndCrossesSkippedFrames() {
        ReplayLaneTimeline timeline = new ReplayLaneTimeline(state(0, 1),
                new ReplayLaneState[]{state(1_000_000, 2), state(2_000_000, 3)});
        assertEquals(1, timeline.at(999_999).config.getHispeed());
        assertEquals(2, timeline.at(1_000_000).config.getHispeed());
        assertEquals(3, timeline.at(5_000_000).config.getHispeed());
    }

    @Test void snapshotRestoresDisplayWithoutChangingJudgmentAndIsIndependent() {
        PlayConfig original = new PlayConfig();
        original.setHispeed(4);
        original.setDuration(321);
        original.setFixhispeed(PlayConfig.FIX_HISPEED_OFF);
        original.setLanecover(0.7f); original.setEnablelanecover(false);
        original.setLift(0.4f); original.setEnablelift(true);
        original.setHidden(0.6f); original.setEnablehidden(true);
        original.setEnableConstant(true); original.setConstantFadeinTime(-50);
        original.setBmsirBaseScrollSpeed(333); original.setBmsirHispeedReferenceBpm(222);
        ReplayLaneState snapshot = ReplayLaneState.capture(1234, original, true);
        original.setHispeed(9);
        PlayConfig target = new PlayConfig();
        target.setJudgetype("Duration");
        snapshot.apply(target);
        assertEquals(4, target.getHispeed()); assertEquals(321, target.getDuration());
        assertEquals(PlayConfig.FIX_HISPEED_OFF, target.getFixhispeed());
        assertEquals(0.7f, target.getLanecover()); assertFalse(target.isEnablelanecover());
        assertEquals(0.4f, target.getLift()); assertTrue(target.isEnablelift());
        assertEquals(0.6f, target.getHidden()); assertTrue(target.isEnablehidden());
        assertTrue(target.isEnableConstant()); assertEquals(-50, target.getConstantFadeinTime());
        assertEquals(333, target.getBmsirBaseScrollSpeed()); assertEquals(222, target.getBmsirHispeedReferenceBpm());
        assertEquals(Boolean.TRUE, target.getBmsirLr2HispeedFixReplayEnabled());
        assertEquals("Duration", target.getJudgetype());
    }

    @Test void invalidOrUnknownDisplayEventsDoNotInvalidateKeyLog() {
        ReplayData replay = new ReplayData();
        replay.keylog = new KeyInputLog[]{new KeyInputLog(100, 0, true)};
        ReplayLaneState bad = state(20, Float.NaN);
        replay.laneStateVersion = 1;
        replay.laneStates = new ReplayLaneState[]{null, state(10, 2), bad, state(9, 3), state(30, 4)};
        assertTrue(replay.validate());
        assertEquals(2, replay.laneStates.length);
        assertEquals(30, replay.laneStates[1].time);
        replay.laneStateVersion = 99;
        assertTrue(replay.validate()); assertNull(replay.laneStates);
        assertEquals(1, replay.keylog.length);
    }

    @Test void oldConfigAndMissingConfigRemainReadable() {
        Json json = new Json();
        ReplayData old = json.fromJson(ReplayData.class,
                "{keylog:[{time:100,keycode:0,pressed:true}],config:{hispeed:3}}" );
        assertTrue(old.validate()); assertEquals(3, old.config.getHispeed()); assertNull(old.laneStates);
        ReplayData older = json.fromJson(ReplayData.class,
                "{keylog:[{time:100,keycode:0,pressed:true}]}" );
        assertTrue(older.validate()); assertNull(older.config);
    }

    @Test void restorationRespectsOptOutNoSpeedAndInvalidInitialState() {
        ReplayData replay = new ReplayData(); replay.config = new PlayConfig();
        CourseData.CourseDataConstraint[] none = new CourseData.CourseDataConstraint[0];
        assertTrue(ReplayLaneState.canRestore(replay, true, none));
        assertFalse(ReplayLaneState.canRestore(replay, false, none));
        assertFalse(ReplayLaneState.canRestore(replay, true,
                new CourseData.CourseDataConstraint[]{CourseData.CourseDataConstraint.NO_SPEED}));
        replay.config.setHispeed(Float.NaN);
        assertFalse(ReplayLaneState.canRestore(replay, true, none));
    }
}
