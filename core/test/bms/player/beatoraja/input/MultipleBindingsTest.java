package bms.player.beatoraja.input;

import bms.player.beatoraja.*;
import bms.model.Mode;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MultipleBindingsTest {
    private static void field(Object object, String name, Object value) throws Exception {
        Class<?> type = object.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(object, value);
                return;
            } catch (NoSuchFieldException missing) { type = type.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }
    private static BMSPlayerInputProcessor input() throws Exception {
        // Avoid the real constructor's controller/MIDI discovery and device opening.
        Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
        BMSPlayerInputProcessor input = (BMSPlayerInputProcessor)((sun.misc.Unsafe)f.get(null)).allocateInstance(BMSPlayerInputProcessor.class);
        field(input, "enable", true); field(input, "keystate", new boolean[256]); field(input, "time", new long[256]);
        field(input, "heldSources", new InputSourceState(258)); field(input, "isAnalog", new boolean[256]);
        field(input, "currentAnalogValue", new float[256]); field(input, "starttime", 1L);
        Class<?> logger = Class.forName("bms.player.beatoraja.input.BMSPlayerInputProcessor$KeyLogger");
        Constructor<?> constructor = logger.getDeclaredConstructor(); constructor.setAccessible(true);
        field(input, "keylog", constructor.newInstance());
        return input;
    }

    @Test void keyboardAliasesAggregateBeforeReportingLogicalEdges() throws Exception {
        BMSPlayerInputProcessor input = input();
        PlayModeConfig.KeyboardConfig config = new PlayModeConfig.KeyboardConfig(Mode.BEAT_7K, false);
        config.getKeyAssign()[0] = Keys.A; config.setDuration(0);
        config.setAdditionalBindings(new int[][]{{Keys.B}});
        Set<Integer> pressed = new HashSet<>(); Input previous = Gdx.input;
        Gdx.input = (Input)Proxy.newProxyInstance(Input.class.getClassLoader(), new Class[]{Input.class}, (proxy, method, args) -> {
            if (method.getName().equals("isKeyPressed")) return pressed.contains((Integer)args[0]);
            if (method.getReturnType() == boolean.class) return false;
            if (method.getReturnType() == int.class) return 0;
            return null;
        });
        try {
            KeyBoardInputProcesseor keyboard = new KeyBoardInputProcesseor(input, config, Resolution.HD);
            keyboard.setConfig(config, true);
            pressed.add(Keys.A); keyboard.poll(100); assertTrue(input.getKeyState(0));
            pressed.add(Keys.B); keyboard.poll(200);
            pressed.remove(Keys.A); keyboard.poll(300); assertTrue(input.getKeyState(0));
            pressed.remove(Keys.B); keyboard.poll(400); assertFalse(input.getKeyState(0));
            assertEquals(2, input.getKeyInputLog().length);
            assertEquals(100, input.getKeyInputLog()[0].getTime()); assertEquals(400, input.getKeyInputLog()[1].getTime());
            long clock = 500;
            for (boolean multi : new boolean[]{true, false}) {
                keyboard.setConfig(config, multi);
                pressed.add(Keys.A); keyboard.poll(clock); assertTrue(input.getKeyState(0));
                keyboard.clear(); // Same timestamp reset performed at READY -> PLAY.
                pressed.remove(Keys.A); keyboard.poll(clock + 100); assertFalse(input.getKeyState(0));
                clock += 200;
            }
            keyboard.setConfig(config, true);
            pressed.add(Keys.B); keyboard.poll(clock);
            keyboard.beginCapturedKeyboardInput(clock + 100); assertFalse(input.getKeyState(0));
            keyboard.poll(clock + 200); assertFalse(input.getKeyState(0));
            pressed.clear(); keyboard.poll(clock + 300); keyboard.poll(clock + 400);
            pressed.add(Keys.B); keyboard.poll(clock + 500); assertTrue(input.getKeyState(0));
        } finally { Gdx.input = previous; }
    }

    @Test void distinctSourcesHoldUntilTheLastReleaseAndDisconnectDoesNotDropOtherInput() throws Exception {
        BMSPlayerInputProcessor input = input(); Object kb = new Object(), controller = new Object(), mouse = new Object();
        input.keyChanged(kb, null, 10, 0, true); input.keyChanged(controller, null, 20, 0, true);
        input.keyChanged(mouse, null, 30, 0, true); input.keyChanged(kb, null, 40, 0, false);
        input.releaseSource(controller); assertTrue(input.getKeyState(0));
        input.keyChanged(mouse, null, 50, 0, false); assertFalse(input.getKeyState(0));
        assertEquals(2, input.getKeyInputLog().length);
        input.startChanged(kb, true); input.startChanged(controller, true); input.startChanged(kb, false);
        assertTrue(input.startPressed()); input.releaseSource(controller); assertFalse(input.startPressed());
        input.selectChanged(kb, true); input.selectChanged(mouse, true); input.selectChanged(kb, false);
        assertTrue(input.isSelectPressed()); input.setSelectPressed(false); assertFalse(input.isSelectPressed());
    }

    @Test void configurationValidationKeepsCrossDevicePrimariesAndFiltersDuplicateAliases() {
        PlayModeConfig mode = new PlayModeConfig(Mode.BEAT_7K);
        mode.getKeyboardConfig().getKeyAssign()[0] = Keys.A;
        mode.getController()[0].getKeyAssign()[0] = 0;
        mode.getKeyboardConfig().setAdditionalBindings(new int[][]{{Keys.B, Keys.B, Keys.A, Keys.F5}, {Keys.B}});
        mode.validate(9);
        assertEquals(0, mode.getController()[0].getKeyAssign()[0]);
        assertArrayEquals(new int[]{Keys.B}, mode.getKeyboardConfig().getAdditionalBindings()[0]);
        assertEquals(0, mode.getKeyboardConfig().getAdditionalBindings()[1].length);
        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
        mode.setMultiInputEnabled(true);
        PlayModeConfig restored = json.fromJson(PlayModeConfig.class, json.toJson(mode));
        restored.setMultiInputEnabled(false); restored.validate(9); restored.setMultiInputEnabled(true);
        assertArrayEquals(new int[]{Keys.B}, restored.getKeyboardConfig().getAdditionalBindings()[0]);
        assertEquals(0, restored.getController()[0].getKeyAssign()[0]);
    }

    @Test void onePhysicalCodeCannotMapToMultipleLogicalKeys() {
        int[][] resolved = MultiInputBindings.resolve(new int[]{Keys.A, Keys.A}, -1, -1,
                new int[][]{{Keys.B}, {Keys.B, Keys.C}}, true);
        assertArrayEquals(new int[]{Keys.A, Keys.B}, resolved[0]);
        assertArrayEquals(new int[]{Keys.C}, resolved[1]);
    }

    @Test void controllerAliasesAndKeyboardSourcePreserveTheSameHold() throws Exception {
        BMSPlayerInputProcessor input = input();
        PlayModeConfig.ControllerConfig config = new PlayModeConfig.ControllerConfig(Mode.BEAT_7K, 0, false);
        Arrays.fill(config.getKeyAssign(), -1); config.getKeyAssign()[0] = 0; config.setDuration(0);
        config.setAdditionalBindings(new int[][]{{1}});
        Set<Integer> held = new HashSet<>();
        com.badlogic.gdx.controllers.Controller device = (com.badlogic.gdx.controllers.Controller)Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class[]{com.badlogic.gdx.controllers.Controller.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getButton")) return held.contains((Integer)args[0]);
                    if (method.getName().equals("getAxis")) return 0f;
                    if (method.getName().equals("getName")) return "fixture";
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    return null;
                });
        BMControllerInputProcessor controller = new BMControllerInputProcessor(input, "fixture", device, config);
        controller.setConfig(config, true); controller.setEnable(true);
        held.add(0); controller.poll(10); held.add(1); controller.poll(20);
        held.remove(0); controller.poll(30); assertTrue(input.getKeyState(0));
        Object keyboard = new Object(); input.keyChanged(keyboard, null, 40, 0, true);
        held.remove(1); controller.poll(50); assertTrue(input.getKeyState(0));
        input.keyChanged(keyboard, null, 60, 0, false); assertFalse(input.getKeyState(0));
        assertEquals(2, input.getKeyInputLog().length);
        long clock = 100;
        for (boolean multi : new boolean[]{true, false}) {
            controller.setConfig(config, multi);
            held.add(0); controller.poll(clock); assertTrue(input.getKeyState(0));
            controller.clear(); held.remove(0); controller.poll(clock + 100); assertFalse(input.getKeyState(0));
            clock += 200;
        }
    }

    @Test void midiAliasesAggregateIndependentlyWithoutOpeningDevices() throws Exception {
        BMSPlayerInputProcessor input = input();
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); unsafeField.setAccessible(true);
        MidiInputProcessor midi = (MidiInputProcessor)((sun.misc.Unsafe)unsafeField.get(null)).allocateInstance(MidiInputProcessor.class);
        field(midi, "bmsPlayerInputProcessor", input);
        field(midi, "heldSources", new ArrayList<>());
        Class<?> handler = Class.forName("bms.player.beatoraja.input.MidiInputProcessor$KeyHandler");
        field(midi, "keyMap", Array.newInstance(handler, 128));
        field(midi, "lastPressedKey", new PlayModeConfig.MidiConfig.Input());
        PlayModeConfig.MidiConfig config = new PlayModeConfig.MidiConfig(Mode.BEAT_7K, false);
        config.setAdditionalBindings(new PlayModeConfig.MidiConfig.Input[][]{{
                new PlayModeConfig.MidiConfig.Input(PlayModeConfig.MidiConfig.Input.Type.NOTE, 60),
                new PlayModeConfig.MidiConfig.Input(PlayModeConfig.MidiConfig.Input.Type.NOTE, 61)}});
        midi.setConfig(config, true);
        midi.noteOn(60); midi.noteOn(61); midi.noteOff(60); assertTrue(input.getKeyState(0));
        midi.noteOff(61); assertFalse(input.getKeyState(0));
        assertEquals(2, input.getKeyInputLog().length);
    }
}
