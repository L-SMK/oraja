package bms.player.beatoraja.skin.lua;

import org.luaj.vm2.LuaString;

/** Fixed-size, render-owned cache of immutable Lua string representations. */
final class SkinLuaTextCache {
    static final int MAX_LENGTH = 4096;
    private final String[] keys = new String[64];
    private final LuaString[] values = new LuaString[64];

    LuaString valueOf(String text) {
        if (text == null || text.length() > MAX_LENGTH) return LuaString.valueOf(text);
        int hash = text.hashCode();
        int slot = (hash ^ (hash >>> 16)) & (values.length - 1);
        if (text.equals(keys[slot])) return values[slot];
        LuaString value = LuaString.valueOf(text);
        keys[slot] = text;
        values[slot] = value;
        return value;
    }
}
