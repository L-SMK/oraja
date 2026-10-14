package bms.player.beatoraja.launcher;

import bms.player.beatoraja.*;
import com.badlogic.gdx.utils.Json;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MissingIrConfigurationTest {
    @Test void offlinePluginSettingsSurviveValidationSerializationAndUiSave() {
        IRConfig missing = new IRConfig(); missing.setIrname("missing-fixture-plugin");
        missing.setUserid("fixture-user"); missing.setPassword("fixture-password"); missing.setIrsend(2);
        missing.setImportscore(true); missing.setImportrival(false);
        PlayerConfig player = new PlayerConfig(); player.setIrconfig(new IRConfig[]{missing}); player.validate();
        assertEquals(1, player.getIrconfig().length);
        String encoded = new Json().toJson(player);
        assertFalse(encoded.contains("fixture-password")); assertFalse(encoded.contains("fixture-user"));
        PlayerConfig restored = new Json().fromJson(PlayerConfig.class, encoded); restored.validate();
        List<String> choices = IRConfigurationView.availableAndStoredNames(new String[]{"Available"}, restored.getIrconfig());
        assertEquals(List.of("Available", "missing-fixture-plugin"), choices);
        Map<String, IRConfig> map = new HashMap<>(); map.put(missing.getIrname(), restored.getIrconfig()[0]);
        IRConfig[] saved = IRConfigurationView.configurationsToSave(choices, map, missing.getIrname());
        assertEquals(1, saved.length); assertEquals("fixture-user", saved[0].getUserid());
        assertEquals("fixture-password", saved[0].getPassword()); assertEquals(2, saved[0].getIrsend());
        assertTrue(saved[0].isImportscore()); assertFalse(saved[0].isImportrival());
    }
    @Test void emptyAuthDoesNotEraseOfflineOptionsAndInvalidNamesAreStillRemoved() {
        IRConfig empty = new IRConfig(); empty.setIrname("offline");
        assertEquals(1, IRConfigurationView.configurationsToSave(List.of("offline"), Map.of("offline", empty), "offline").length);
        empty.setIrname(" "); assertFalse(empty.validate());
    }
}
