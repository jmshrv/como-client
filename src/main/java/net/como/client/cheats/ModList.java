package net.como.client.cheats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.font.TextRenderer;
import net.como.client.CheatClient;
import net.como.client.events.InGameHudRenderEvent;
import net.como.client.structures.Cheat;
import net.como.client.structures.events.Event;

public class ModList extends Cheat {

    public ModList() {
        super("ModList");
        
        this.description = "Displays all of your enabled mods";
        this.modListDisplay = false;
    }

    @Override
    public void activate() {
        this.addListen(InGameHudRenderEvent.class);
    }

    @Override
    public void deactivate() {
        this.removeListen(InGameHudRenderEvent.class);
    }

    @Override
    public void fireEvent(Event event) {
        switch (event.getClass().getSimpleName()) {
            case "InGameHudRenderEvent": {
                InGameHudRenderEvent e = (InGameHudRenderEvent)event;

                TextRenderer textRenderer = CheatClient.getClient().textRenderer;
                List<Cheat> enabledMods = new ArrayList<Cheat>();
                
                for (String cheatName : CheatClient.Cheats.keySet()) {
                    Cheat cheat = CheatClient.Cheats.get(cheatName);

                    if (!cheat.shouldDisplayInModList()) continue;
                    
                    enabledMods.add(cheat);
                }

                // Sort the enabledMods list by the cheat name
                Collections.sort(enabledMods, (c1, c2) -> {
                    return c2.getName().length() - c1.getName().length();
                });

                int display = 0;
                for (Cheat cheat : enabledMods) {
                    textRenderer.drawWithShadow(e.mStack, cheat.getName(), 1, 1+10*display, 0xFFFFFFFF);
                    display++;
                }
            }
        } 
    }
}