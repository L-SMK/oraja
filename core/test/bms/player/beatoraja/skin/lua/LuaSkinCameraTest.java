package bms.player.beatoraja.skin.lua;

import bms.player.beatoraja.skin.json.JsonSkin;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import static org.junit.jupiter.api.Assertions.*;

class LuaSkinCameraTest {
    @Test void luaUsesTheSameCameraSchema() {
        LuaTable root = new LuaTable(), cameras = new LuaTable(), camera = new LuaTable();
        camera.set("id", LuaValue.valueOf("hands"));
        camera.set("cropWidth", LuaValue.valueOf(.5));
        cameras.set(1, camera); root.set("camera", cameras);
        var skin = new LuaSkinLoader().fromLuaValue(JsonSkin.Skin.class, root);
        assertEquals("hands", skin.camera[0].id);
        assertEquals(.5f, skin.camera[0].cropWidth);
        assertEquals(1, skin.camera[0].cropHeight);
    }
}
