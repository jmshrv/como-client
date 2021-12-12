package net.como.client.modules;

import net.como.client.ComoClient;
import net.como.client.events.ClientTickEvent;
import net.como.client.events.DeathEvent;
import net.como.client.events.RenderWorldEvent;
import net.como.client.structures.Module;
import net.como.client.structures.events.Event;
import net.como.client.structures.settings.Setting;

public class HomeGodMode extends Module {
    public HomeGodMode() {
        super("HomeGodMode");

        this.addSetting(new Setting("HomeName", "Death"));
        this.addSetting(new Setting("RespawnDelay", 250d));

        this.description = "Exploits the /sethome and /home feature on servers";
    }

    @Override
    public void activate() {
        if (ComoClient.Modules.get("autorespawn").isEnabled()) this.displayMessage("Please disable 'AutoRespawn' as it may mess with respawn times.");

        this.addListen(RenderWorldEvent.class);
    }

    @Override
    public void deactivate() {
        this.removeListen(RenderWorldEvent.class);
    }

    private void setHome() {
        ComoClient.me().sendChatMessage(String.format("/sethome %s", this.getStringSetting("HomeName")));
    }

    private void home() {
        ComoClient.me().sendChatMessage(String.format("/home %s", this.getStringSetting("HomeName")));
    }

    boolean deathTrigger = false;
    
    double deathTime = 0;

    private double nextRespawnTime() {
        return this.deathTime + this.getDoubleSetting("RespawnDelay")/1000d;
    }

    private boolean shouldRespawn() {
        return ComoClient.getCurrentTime() >= this.nextRespawnTime();
    }

    @Override
    public void fireEvent(Event event) {
        switch (event.getClass().getSimpleName()) {
            case "RenderWorldEvent": {
                // Check for the death
                if (ComoClient.me().isAlive()) {
                    if (deathTrigger) {
                        // Do /home
                        this.home();
                    }

                    deathTrigger = false;
                    deathTime = 0;

                    break;
                }

                if (!deathTrigger) {
                    // Set our home
                    this.setHome();

                    deathTrigger = true;
                    deathTime = ComoClient.getCurrentTime();

                    break;
                }

                // See if we should respawn
                if (this.shouldRespawn()) ComoClient.me().requestRespawn();

                break;
            }
        }
    }
}
