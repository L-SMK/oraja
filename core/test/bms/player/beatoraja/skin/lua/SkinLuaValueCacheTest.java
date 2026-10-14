package bms.player.beatoraja.skin.lua;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.skin.CustomTimer;
import bms.player.beatoraja.skin.Skin;
import bms.player.beatoraja.skin.SkinHeader;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.JsePlatform;

import static org.junit.jupiter.api.Assertions.*;

class SkinLuaValueCacheTest {
    @Test void scalarRepresentationMatchesLuaJIncludingSentinelsAndSpecialValues() {
        var cache = new SkinLuaValueCache(128);
        for (double value : new double[] { Long.MIN_VALUE, Long.MAX_VALUE, Integer.MIN_VALUE,
                -1000, 0, -0.0, 1000, 10000000.25, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
            LuaValue expected = LuaNumber.valueOf(value);
            LuaValue actual = cache.number(7, value);
            assertEquals(expected.getClass(), actual.getClass());
            assertEquals(Double.doubleToRawLongBits(expected.todouble()),
                    Double.doubleToRawLongBits(actual.todouble()));
            assertSame(actual, cache.number(7, value));
        }
        LuaValue first = cache.number(7, 10000000);
        cache.number(135, 20000000); // Direct-map collision, never return another ID's value.
        assertEquals(10000000, cache.number(7, 10000000).toint());
        assertNotSame(first, cache.number(7, 10000000));
    }

    @Test void typeFunctionKeepsStandardBehaviorAndAllowsUserOverride() {
        Globals stock = JsePlatform.standardGlobals();
        Globals cached = JsePlatform.standardGlobals();
        SkinLuaValueCache.installTypeFunction(cached);
        for (String expression : new String[] { "nil", "true", "3.5", "'x'", "{}",
                "function() end", "coroutine.create(function() end)" }) {
            String script = "return type(" + expression + ")";
            assertEquals(stock.load(script).call(), cached.load(script).call());
        }
        LuaValue userdata = LuaValue.userdataOf(new Object());
        assertEquals(stock.get("type").call(userdata), cached.get("type").call(userdata));
        assertSame(cached.get("type").call(LuaValue.valueOf(1000)),
                cached.get("type").call(LuaValue.valueOf(2000)));
        assertEquals(stock.get("type").tojstring(), cached.get("type").tojstring());
        assertThrows(LuaError.class, () -> stock.get("type").call());
        assertThrows(LuaError.class, () -> cached.get("type").call());
        assertEquals("custom", cached.load("type=function() return 'custom' end; return type(1)").call().tojstring());
        var accessor = new SkinLuaAccessor(false);
        assertEquals("number", accessor.exec("return type(1)").tojstring());
    }

    @Test void exportedPropertiesReadLiveStateEvenWithinTheSameFrame() {
        var state = new TestState();
        var exports = new LuaTable();
        new MainStateAccessor(state).export(exports);
        LuaValue timer = exports.get("timer");
        LuaValue id = LuaValue.valueOf(40);
        state.timer.setMicroTimer(40, 10000000);
        LuaValue initial = timer.call(id);
        assertSame(initial, timer.call(id));
        state.timer.setMicroTimer(40, 10000001);
        assertEquals(10000001, timer.call(id).toint());
        state.timer.setTimerOff(40);
        assertEquals((double) Long.MIN_VALUE, timer.call(id).todouble());
        state.timer.setMicroTimer(19000, 20000000);
        assertEquals(20000000, timer.call(LuaValue.valueOf(19000)).toint());
        state.timer.setMicroTimer(19000, 20000001);
        assertEquals(20000001, timer.call(LuaValue.valueOf(19000)).toint());
        LuaValue now = exports.get("time").call();
        assertSame(now, exports.get("time").call());
        state.count = 5000;
        assertEquals(5000, exports.get("number").call(LuaValue.valueOf(110)).toint());
        state.count = 5001;
        assertEquals(5001, exports.get("number").call(LuaValue.valueOf(110)).toint());
        assertSame(now, exports.get("time").call()); // Timer/score change at an unchanged clock.
        state.timer.update();
        assertEquals((double) state.timer.getNowMicroTime(), exports.get("time").call().todouble());
    }

    private static final class TestState extends MainState {
        private final Skin skin;
        int count;
        TestState() {
            super(null, null);
            var header = new SkinHeader();
            header.setSourceResolution(Resolution.FULLHD);
            header.setDestinationResolution(Resolution.FULLHD);
            skin = new Skin(header);
            skin.addCustomTimer(new CustomTimer(19000, null));
        }
        @Override public Skin getSkin() { return skin; }
        @Override public int getJudgeCount(int judge, boolean fast) { return fast ? count : 0; }
        @Override public void create() {}
        @Override public void render() {}
    }
}
