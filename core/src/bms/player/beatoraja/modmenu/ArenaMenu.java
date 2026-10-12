package bms.player.beatoraja.modmenu;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.arena.client.ArenaBar;
import bms.player.beatoraja.arena.bmsir.BMSIRArenaI18n;
import bms.player.beatoraja.arena.client.Client;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.song.SongData;

/**
 * Chart-selection helpers shared with the BMS-IR Arena client. The legacy
 * EndlessDream ArenaEX window that used to live here has been removed.
 */
public class ArenaMenu {
    private static MusicSelector selector;

    private static String t(String japanese, String english) {
        return BMSIRArenaI18n.text(japanese, english);
    }

    static {
        MainController.registerBeforeRenderTask((main) -> {
            if (Client.state.getAutoSelectFlag()) {
                selectCurrentLobbySong();
                Client.state.setAutoSelectFlag(false);
            }
        });
    }

    public static void init(String username, MusicSelector selector) {
        Client.userName.set(username);
        ArenaMenu.selector = selector;
    }

    /**
     * Auto select the current chosen song in lobby
     *
     * @implSpec This function must be called in game's main thread, otherwise the game crashes immediately
     *  because we cannot dispose a texture outside of glfw context
     */
    public static void selectCurrentLobbySong() {
        SongData songData = Client.state.getCurrentSongData();
        if (songData != null) {
            ArenaBar bar = new ArenaBar(selector, songData);
            selector.getBarManager().replaceArenaSelection(bar);
            selector.getBarManager().updateBar();
            selector.getBarManager().setSelected(bar);
            // This line might break something if we're not currently at music select scene
            selector.getBarManager().updateBar(bar);
        }
    }

    /**
     * Start the BMS-IR selected Arena chart as an ordinary single-song play.
     * This must run on the libGDX main thread.
     */
    public static void startCurrentLobbySong() {
        SongData songData = Client.state.getCurrentSongData();
        if (songData == null || selector == null) {
            ImGuiNotify.error(t(
                    "BMS-IR Arena: 課題譜面を開始できません",
                    "BMS-IR Arena: Could not start the selected chart"
            ));
            return;
        }
        ArenaBar bar = new ArenaBar(selector, songData);
        selector.readArenaChart(songData, bar);
    }

    public static void refreshMissingChartState() {
        MainController.pushOneShotAfterRenderTask(main -> {
            String md5 = Client.state.getSelectedSongRemote().getMd5();
            SongData[] songDatas = main.getSongDatabase().getSongDatas(new String[]{md5});
            if (songDatas != null && songDatas.length > 0) {
                Client.state.setLobbySongData(songDatas[0]);
            }
        });
    }
}
