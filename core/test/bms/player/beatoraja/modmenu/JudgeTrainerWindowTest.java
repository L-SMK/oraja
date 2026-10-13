package bms.player.beatoraja.modmenu;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;
import org.junit.jupiter.api.Test;

class JudgeTrainerWindowTest {
    @Test void collapsedAndExpandedCpuFramesHaveBalancedWindowStacks() {
        // No platform backend, OS window, renderer or game body is initialized.
        ImGui.createContext();
        try {
            ImGui.getIO().setIniFilename(null);
            ImGui.getIO().setDisplaySize(1280, 720); ImGui.getIO().setDeltaTime(1f / 60);
            ImGui.getIO().getFonts().addFontDefault(); ImGui.getIO().getFonts().build();
            for (boolean collapsed : new boolean[]{true, false, true}) {
                ImGui.newFrame(); ImGui.setNextWindowCollapsed(collapsed, ImGuiCond.Always);
                JudgeTrainerMenu.show(new ImBoolean(true)); ImGui.render();
            }
        } finally { ImGui.destroyContext(); }
    }
}
