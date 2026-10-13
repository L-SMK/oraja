package bms.player.beatoraja;

import bms.model.BMSDecoder;
import bms.model.BMSModel;
import bms.player.beatoraja.song.SongData;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Local storage only: the chart's public MD5/SHA and IR identity stay authored. */
public final class BMSIRParserCompatibility {
    public static final int CURRENT_REVISION = 2;
    public static final int AFFECTED_METADATA = 3;
    private BMSIRParserCompatibility() {}

    public static boolean affected(BMSModel model) {
        return model != null && "2".equals(model.getValues().get(BMSDecoder.COMPATIBILITY_REVISION))
                && "1".equals(model.getValues().get(BMSDecoder.COMPATIBILITY_AFFECTED));
    }
    public static boolean affected(SongData song) {
        return song.getParsercompatibility() == AFFECTED_METADATA;
    }
    public static String storageHash(BMSModel model, String base) { return storageHash(base, affected(model)); }
    public static String storageHash(SongData song, String base) { return storageHash(base, affected(song)); }
    private static String storageHash(String base, boolean affected) {
        if (!affected) return base;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(("bmsir-parser-v2:" + base).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
