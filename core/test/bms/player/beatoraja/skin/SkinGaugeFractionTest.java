package bms.player.beatoraja.skin;

import bms.player.beatoraja.Resolution;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.junit.jupiter.api.Test;

import static bms.player.beatoraja.skin.SkinProperty.*;
import static org.junit.jupiter.api.Assertions.*;

class SkinGaugeFractionTest {
    @Test
    void separateDecimalReferencePreservesFractionalSkinsAndInvalidatesOnRemoval() {
        SkinHeader header = new SkinHeader();
        header.setSourceResolution(Resolution.SD);
        header.setDestinationResolution(Resolution.SD);
        Skin skin = new Skin(header);
        SkinNumber integer = number(NUMBER_GROOVEGAUGE);
        SkinNumber fraction = number(NUMBER_GROOVEGAUGE_AFTERDOT);
        skin.add(integer);
        assertFalse(skin.hasGaugeFraction());
        skin.add(fraction);
        assertTrue(skin.hasGaugeFraction());
        assertTrue(skin.hasGaugeFraction());
        skin.removeSkinObject(fraction);
        assertFalse(skin.hasGaugeFraction());
    }

    private static SkinNumber number(int reference) {
        return new SkinNumber(new TextureRegion[1][10], 0, 0, 1, 0, 0, reference, 0);
    }
}
