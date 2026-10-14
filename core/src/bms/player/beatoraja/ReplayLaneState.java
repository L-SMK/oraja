package bms.player.beatoraja;

/** Display-only replay state. It never restores judgment or chart options. */
public final class ReplayLaneState {
    public long time;
    public PlayConfig config;

    public static ReplayLaneState capture(long time, PlayConfig live, boolean lr2Fixed) {
        ReplayLaneState state = new ReplayLaneState();
        state.time = Math.max(0, time);
        state.config = live.clone();
        state.config.setBmsirLr2HispeedFixReplayEnabled(lr2Fixed);
        return state;
    }

    public boolean valid() {
        return time >= 0 && validConfig(config);
    }
    public static boolean validConfig(PlayConfig config) {
        return config != null && Float.isFinite(config.getHispeed()) && config.getHispeed() > 0
                && (!config.isBmsirFloatingHispeedEnabled()
                    || (Float.isFinite(config.getBmsirFloatingHispeed())
                        && Float.isFinite(config.getBmsirFloatingGreenFrames())))
                && Float.isFinite(config.getLanecover())
                && Float.isFinite(config.getLift()) && Float.isFinite(config.getHidden());
    }

    public static boolean canRestore(ReplayData replay, boolean enabled, CourseData.CourseDataConstraint[] constraints) {
        if (!enabled || replay == null || !validConfig(replay.config)) return false;
        for (CourseData.CourseDataConstraint constraint : constraints) {
            if (constraint == CourseData.CourseDataConstraint.NO_SPEED) return false;
        }
        return true;
    }

    public void apply(PlayConfig target) {
        PlayConfig source = config.clone();
        source.validate();
        target.setHispeed(source.getHispeed());
        target.setDuration(source.getDuration());
        target.setFixhispeed(source.getFixhispeed());
        target.setLanecover(source.getLanecover());
        target.setEnablelanecover(source.isEnablelanecover());
        target.setLift(source.getLift());
        target.setEnablelift(source.isEnablelift());
        target.setHidden(source.getHidden());
        target.setEnablehidden(source.isEnablehidden());
        target.setEnableConstant(source.isEnableConstant());
        target.setConstantFadeinTime(source.getConstantFadeinTime());
        target.setBmsirBaseScrollSpeed(source.getBmsirBaseScrollSpeed());
        target.setBmsirHispeedReferenceBpm(source.getBmsirHispeedReferenceBpm());
        target.setBmsirLr2HispeedFixReplayEnabled(source.getBmsirLr2HispeedFixReplayEnabled());
        target.setBmsirFloatingHispeedEnabled(source.isBmsirFloatingHispeedEnabled());
        target.setBmsirFloatingHispeed(source.getBmsirFloatingHispeed());
        target.setBmsirFloatingGreenFrames(source.getBmsirFloatingGreenFrames());
        target.setBmsirFloatingCoverVisible(source.isBmsirFloatingCoverVisible());
        target.setBmsirFloatingSuddenLiftVisiblePixels(source.getBmsirFloatingSuddenLiftVisiblePixels());
    }

    public boolean sameDisplay(ReplayLaneState other) {
        if (other == null || config == null || other.config == null) return false;
        PlayConfig a = config, b = other.config;
        return a.getHispeed() == b.getHispeed() && a.getDuration() == b.getDuration()
                && a.getFixhispeed() == b.getFixhispeed()
                && a.isBmsirFloatingHispeedEnabled() == b.isBmsirFloatingHispeedEnabled()
                && a.getBmsirFloatingHispeed() == b.getBmsirFloatingHispeed()
                && a.getBmsirFloatingGreenFrames() == b.getBmsirFloatingGreenFrames()
                && a.isBmsirFloatingCoverVisible() == b.isBmsirFloatingCoverVisible()
                && a.getBmsirFloatingSuddenLiftVisiblePixels() == b.getBmsirFloatingSuddenLiftVisiblePixels()
                && a.getLanecover() == b.getLanecover() && a.isEnablelanecover() == b.isEnablelanecover()
                && a.getLift() == b.getLift() && a.isEnablelift() == b.isEnablelift()
                && a.getHidden() == b.getHidden() && a.isEnablehidden() == b.isEnablehidden()
                && a.isEnableConstant() == b.isEnableConstant()
                && a.getConstantFadeinTime() == b.getConstantFadeinTime()
                && a.getBmsirBaseScrollSpeed() == b.getBmsirBaseScrollSpeed()
                && a.getBmsirHispeedReferenceBpm() == b.getBmsirHispeedReferenceBpm()
                && java.util.Objects.equals(a.getBmsirLr2HispeedFixReplayEnabled(), b.getBmsirLr2HispeedFixReplayEnabled());
    }
}
