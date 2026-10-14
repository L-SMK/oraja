package bms.player.beatoraja.skin.lua;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.ZeroArgFunction;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class SkinLuaTextCacheTest {
    // LuaJ's parent/subclass static initialization is not safe when first touched in parallel.
    // Initialize the parent first, matching normal single-threaded skin startup.
    static { LuaValue.valueOf(0); }

    @Test void contentReuseAndCollisionsKeepCurrentText() {
        var cache = new SkinLuaTextCache();
        LuaString first = cache.valueOf("曲名・Artist");
        assertSame(first, cache.valueOf(new String("曲名・Artist")));
        assertEquals("変更後", cache.valueOf("変更後").tojstring());
        assertEquals("Aa".hashCode(), "BB".hashCode());
        assertEquals("Aa", cache.valueOf("Aa").tojstring());
        assertEquals("BB", cache.valueOf("BB").tojstring());
        assertEquals("Aa", cache.valueOf("Aa").tojstring());
        for (int i = 0; i < 8000; i++) assertEquals("title-" + i, cache.valueOf("title-" + i).tojstring());
        assertEquals("曲名・Artist", cache.valueOf("曲名・Artist").tojstring());
    }

    @Test void encodingAndLongTextFollowOriginalLuaJConversion() {
        var cache = new SkinLuaTextCache();
        for (String text : new String[] { "", "ASCII", "譜面\nタイトル", "é☃😀", "\u0000",
                "x".repeat(SkinLuaTextCache.MAX_LENGTH), "x".repeat(SkinLuaTextCache.MAX_LENGTH + 1) }) {
            assertEquals(LuaString.valueOf(text), cache.valueOf(text));
        }
        assertThrows(NullPointerException.class, () -> cache.valueOf(null));
    }

    @Test void decodingReuseStillCallsTheFunctionAndObservesSameFrameChanges() {
        var calls = new AtomicInteger();
        var value = new AtomicReference<LuaValue>(LuaString.valueOf("same title"));
        var accessor = new SkinLuaAccessor(false);
        var property = accessor.loadStringProperty(new ZeroArgFunction() {
            @Override public LuaValue call() { calls.incrementAndGet(); return value.get(); }
        });
        String first = property.get(null);
        assertSame(first, property.get(null));
        assertEquals(2, calls.get());
        value.set(LuaString.valueOf("new title"));
        assertEquals("new title", property.get(null));
        assertEquals(3, calls.get());
        value.set(LuaValue.valueOf(12345));
        assertEquals("12345", property.get(null));
        value.set(LuaValue.NIL);
        assertEquals("nil", property.get(null));
        value.set(LuaString.valueOf("same title"));
        assertEquals("same title", property.get(null));
        assertEquals(6, calls.get());
    }

    @Test void mutableNonStringsAreNeverMemoized() {
        var calls = new AtomicInteger();
        LuaValue userdata = new LuaUserdata(new Object()) {
            @Override public String tojstring() { return "value-" + calls.incrementAndGet(); }
        };
        var accessor = new SkinLuaAccessor(false);
        var property = accessor.loadStringProperty(new ZeroArgFunction() {
            @Override public LuaValue call() { return userdata; }
        });
        assertEquals("value-1", property.get(null));
        assertEquals("value-2", property.get(null));
    }
}
