package bms.player.beatoraja.skin.lua;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.LibFunction;

/** Reuses immutable Lua values, never the underlying live skin state. Render-thread owned. */
final class SkinLuaValueCache {

    private final int[] keys;
    private final long[] bits;
    private final LuaValue[] values;

    SkinLuaValueCache(int capacity) {
        if (capacity < 1 || Integer.bitCount(capacity) != 1) throw new IllegalArgumentException();
        keys = new int[capacity];
        bits = new long[capacity];
        values = new LuaValue[capacity];
    }

    LuaValue number(int key, double value) {
        int slot = key & (values.length - 1);
        long rawBits = Double.doubleToRawLongBits(value);
        if (values[slot] != null && keys[slot] == key && bits[slot] == rawBits) return values[slot];
        LuaValue result = LuaNumber.valueOf(value);
        keys[slot] = key;
        bits[slot] = rawBits;
        values[slot] = result;
        return result;
    }

    /** LuaJ's standard type() rebuilds a char[] and byte[] even for a constant type name. */
    static void installTypeFunction(Globals globals) {
        globals.set("type", new LibFunction() {
            private final LuaString nil = LuaString.valueOf("nil");
            private final LuaString bool = LuaString.valueOf("boolean");
            private final LuaString number = LuaString.valueOf("number");
            private final LuaString string = LuaString.valueOf("string");
            private final LuaString table = LuaString.valueOf("table");
            private final LuaString function = LuaString.valueOf("function");
            private final LuaString userdata = LuaString.valueOf("userdata");
            private final LuaString thread = LuaString.valueOf("thread");
            { name = "type"; }

            @Override public LuaValue call(LuaValue value) {
                String typeName = value.typename();
                return switch (typeName) {
                    case "nil" -> nil;
                    case "boolean" -> bool;
                    case "number" -> number;
                    case "string" -> string;
                    case "table" -> table;
                    case "function" -> function;
                    case "userdata" -> userdata;
                    case "thread" -> thread;
                    default -> LuaString.valueOf(typeName);
                };
            }
        });
    }
}
