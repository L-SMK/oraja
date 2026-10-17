package bms.player.beatoraja.play;

import bms.player.beatoraja.ReplayLaneState;
import java.util.ArrayList;

/** Uses chart microseconds, independently of key-log margins and wall time. */
public final class ReplayLaneTimeline {
    public static final int MAX_EVENTS = 100_000;
    private final ArrayList<ReplayLaneState> recorded = new ArrayList<>();
    private final ReplayLaneState[] playback;
    private ReplayLaneState current;
    private int cursor;

    public ReplayLaneTimeline(ReplayLaneState initial, ReplayLaneState[] playback) {
        this.current = initial;
        this.playback = playback == null ? new ReplayLaneState[0] : playback;
    }

    public void record(ReplayLaneState state) {
        if (!state.valid() || state.sameDisplay(current)) return;
        if (!recorded.isEmpty() && state.time < recorded.get(recorded.size() - 1).time) return;
        if (!recorded.isEmpty() && state.time == recorded.get(recorded.size() - 1).time) {
            recorded.set(recorded.size() - 1, state);
        } else if (recorded.size() < MAX_EVENTS) {
            recorded.add(state);
        } else return;
        current = state;
    }

    public ReplayLaneState at(long chartTime) {
        while (cursor < playback.length && playback[cursor].time <= chartTime) {
            current = playback[cursor++];
        }
        return current;
    }

    public ReplayLaneState[] recorded() {
        return recorded.toArray(ReplayLaneState[]::new);
    }
}
