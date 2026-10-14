package bms.player.beatoraja.config;

import bms.player.beatoraja.PlayModeConfig;
import bms.model.Mode;
import com.badlogic.gdx.Input.Keys;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import static org.junit.jupiter.api.Assertions.*;

class KeyConfigurationAdditionalMouseTest {
    @Test void mouseMovementDuringAddDoesNotReplaceExistingBindingsOrEndCapture() throws Exception {
        Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
        KeyConfiguration view = (KeyConfiguration)((sun.misc.Unsafe)f.get(null)).allocateInstance(KeyConfiguration.class);
        Field adding = KeyConfiguration.class.getDeclaredField("appendBinding"); adding.setAccessible(true); adding.set(view, true);
        PlayModeConfig.KeyboardConfig config = new PlayModeConfig.KeyboardConfig(Mode.BEAT_7K, false);
        config.getKeyAssign()[0] = Keys.A; config.setAdditionalBindings(new int[][]{{Keys.B}});
        Field keyboard = KeyConfiguration.class.getDeclaredField("keyboardConfig"); keyboard.setAccessible(true); keyboard.set(view, config);
        Method assign = KeyConfiguration.class.getDeclaredMethod("setMouseScratchKeyAssign", int.class,
                bms.player.beatoraja.input.KeyBoardInputProcesseor.class);
        assign.setAccessible(true);
        assertEquals(false, assign.invoke(view, 0, null));
        assertEquals(Keys.A, config.getKeyAssign()[0]);
        assertArrayEquals(new int[]{Keys.B}, config.getAdditionalBindings()[0]);
    }
}
