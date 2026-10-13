package bms.player.beatoraja.input;

import java.util.IdentityHashMap;

/** OR aggregation of held logical keys; sources use identity, not device names. */
public final class InputSourceState {
    private final IdentityHashMap<Object, boolean[]> sources = new IdentityHashMap<>();
    private final int size;
    public InputSourceState(int size) { this.size = size; }

    public synchronized boolean update(Object source, int key, boolean pressed) {
        if (key < 0 || key >= size) return false;
        boolean[] held = sources.computeIfAbsent(source, ignored -> new boolean[size]);
        held[key] = pressed;
        return held(key);
    }

    public synchronized boolean held(int key) {
        for (boolean[] held : sources.values()) if (held[key]) return true;
        return false;
    }

    public synchronized void remove(Object source) { sources.remove(source); }
    public synchronized void clear() { sources.clear(); }
    public synchronized void clearKey(int key) {
        for (boolean[] held : sources.values()) held[key] = false;
    }
}
