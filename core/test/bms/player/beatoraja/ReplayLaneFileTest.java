package bms.player.beatoraja;

import bms.model.*;
import bms.player.beatoraja.input.KeyInputLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.charset.Charset;
import static org.junit.jupiter.api.Assertions.*;

class ReplayLaneFileTest {
    @TempDir Path directory;
    private ReplayData replay(float hs) {
        ReplayData replay = new ReplayData();
        replay.keylog = new KeyInputLog[]{new KeyInputLog(1_000_000, 0, true), new KeyInputLog(1_100_000, 0, false)};
        replay.config = new PlayConfig(); replay.config.setHispeed(hs);
        replay.laneStateVersion = 1;
        PlayConfig next = replay.config.clone(); next.setHispeed(hs + 1); next.setLanecover(0.5f);
        replay.laneStates = new ReplayLaneState[]{ReplayLaneState.capture(1_500_000, next, true)};
        return replay;
    }
    @Test void singleAndCourseFilesRoundTripDistinctStages() throws Exception {
        Config config = new Config(); config.setPlayerpath(directory.toString()); config.setPlayername("fixture");
        Files.createDirectories(directory.resolve("fixture"));
        PlayDataAccessor accessor = new PlayDataAccessor(config);
        BMSModel model = new BMSDecoder().decode(("#PLAYER 1\n#TITLE Test\n#BPM 120\n#WAV01 a.wav\n#00111:01\n")
                .getBytes(Charset.forName("MS932")), false, null);
        String hash = model.getSHA256();
        accessor.wrireReplayData(replay(2), model, 0, 0);
        ReplayData loaded = accessor.readReplayData(model, 0, 0);
        assertNotNull(loaded); assertEquals(2, loaded.config.getHispeed());
        assertEquals(3, loaded.laneStates[0].config.getHispeed());
        assertEquals(1_500_000, loaded.laneStates[0].time);
        assertEquals(2, loaded.keylog.length); assertEquals(1_100_000, loaded.keylog[1].getTime());
        ReplayData[] stages = {replay(3), replay(6)};
        String[] hashes = {hash, hash};
        accessor.wrireReplayData(stages, hashes, false, 0, 1, new CourseData.CourseDataConstraint[0]);
        ReplayData[] course = accessor.readReplayData(hashes, false, 0, 1, new CourseData.CourseDataConstraint[0]);
        assertNotNull(course); assertEquals(2, course.length);
        assertEquals(4, course[0].laneStates[0].config.getHispeed());
        assertEquals(7, course[1].laneStates[0].config.getHispeed());
        assertEquals(hash, model.getSHA256());
    }
}
