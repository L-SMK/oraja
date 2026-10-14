package bms.player.beatoraja.skin.json;

import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.lua.*;
import com.badlogic.gdx.utils.Json;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class JsonSkinCameraTest {
    @Test void jsonOptionsCanSelectCameraDeclarationsWithoutOpeningHardware() {
        Json json = new Json();
        new JsonSkinSerializer(new SkinLuaAccessor(true), path -> Path.of(path).toFile())
                .setSerializers(json, new HashSet<>(java.util.Set.of(900)), Path.of("."));
        JsonSkin.Skin skin = json.fromJson(JsonSkin.Skin.class,
                "{camera:[{if:900,value:{id:hands,cropX:0.25,cropY:0.25,cropWidth:0.5,cropHeight:0.5}},"
                        + "{if:901,value:{id:hidden}}],destination:[{id:hands,stretch:3,dst:[{x:100,y:100,w:320,h:180}]}]}");
        assertEquals(1, skin.camera.length);
        assertEquals("hands", skin.camera[0].id);
        var loader = new JsonPlaySkinObjectLoader(new JSONSkinLoader());
        var object = loader.loadSkinObject(null, skin, skin.destination[0], Path.of("."));
        assertInstanceOf(SkinCamera.class, object);
        assertEquals(StretchType.KEEP_ASPECT_RATIO_FIT_INNER, object.getStretch());
        assertEquals(3, skin.destination[0].stretch);
        assertEquals(320, skin.destination[0].dst[0].w);
    }
    @Test void oldSkinsHaveNoCamera() {
        assertEquals(0, new Json().fromJson(JsonSkin.Skin.class, "{}").camera.length);
    }
}
