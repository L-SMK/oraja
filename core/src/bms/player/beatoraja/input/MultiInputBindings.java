package bms.player.beatoraja.input;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Stable, bounded binding lists: lane indices followed by START and SELECT. */
public final class MultiInputBindings {
    public static final int MAX_ADDITIONAL = 7;
    private MultiInputBindings() {}

    public static int[][] normalize(int[] primary, int[][] extra, int maxCode) {
        int[][] result = new int[primary.length][];
        Set<Integer> used = new HashSet<>();
        for (int code : primary) if (code >= 0 && code < maxCode) used.add(code);
        for (int index = 0; index < result.length; index++) {
            int[] row = extra != null && index < extra.length && extra[index] != null
                    ? extra[index] : new int[0];
            int[] accepted = new int[MAX_ADDITIONAL];
            int count = 0;
            for (int code : row) {
                if (code >= 0 && code < maxCode && count < MAX_ADDITIONAL && used.add(code)) accepted[count++] = code;
            }
            result[index] = Arrays.copyOf(accepted, count);
        }
        return result;
    }

    public static int[][] resolve(int[] keys, int start, int select, int[][] extra, boolean enabled) {
        int[] primary = Arrays.copyOf(keys, keys.length + 2);
        primary[keys.length] = start;
        primary[keys.length + 1] = select;
        int[][] normalized = normalize(primary, enabled ? extra : null, 256);
        int[][] result = new int[primary.length][];
        Set<Integer> assigned = new HashSet<>();
        for (int index = 0; index < primary.length; index++) {
            int[] codes = new int[normalized[index].length + 1];
            int count = 0;
            if (primary[index] >= 0 && primary[index] < 256 && assigned.add(primary[index])) codes[count++] = primary[index];
            for (int code : normalized[index]) if (assigned.add(code)) codes[count++] = code;
            result[index] = Arrays.copyOf(codes, count);
        }
        return result;
    }

    public static boolean pressed(int[] codes, boolean[] physical) {
        for (int code : codes) if (code >= 0 && code < physical.length && physical[code]) return true;
        return false;
    }

    public static int[][] add(int[][] extra, int rows, int index, int code) {
        int[][] result = new int[rows][];
        for (int row = 0; row < rows; row++) {
            result[row] = extra != null && row < extra.length && extra[row] != null ? extra[row].clone() : new int[0];
        }
        if (index < 0 || index >= rows || result[index].length >= MAX_ADDITIONAL) return result;
        if (Arrays.stream(result[index]).anyMatch(value -> value == code)) return result;
        result[index] = Arrays.copyOf(result[index], result[index].length + 1);
        result[index][result[index].length - 1] = code;
        return result;
    }
}
