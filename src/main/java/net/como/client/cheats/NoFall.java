package net.como.client.cheats;

import net.como.client.CheatClient;
import net.como.client.events.MovementPacketEvent;
import net.como.client.structures.Cheat;
import net.como.client.structures.events.Event;
import net.como.client.utils.ClientUtils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class NoFall extends Cheat {
    public NoFall() {
        super("NoFall");

        this.description = "Take less fall damage.";
    }

    @Override
    public void activate() {
        this.addListen(MovementPacketEvent.class);
    }

    @Override
    public void deactivate() {
        this.removeListen(MovementPacketEvent.class);
    }

    @Override
    public void fireEvent(Event event) {
        switch (event.getClass().getSimpleName()) {
            case "MovementPacketEvent": {
                // Get the localplayer.
                ClientPlayerEntity player = CheatClient.me();

                // Use to make sure that flight is less gittery
                if(player.fallDistance <= (player.isFallFlying() ? 1 : 2)) break;

                if (player.isFallFlying()) {
                    // Make sure that the player is not falling too quickly
                    if(player.isSneaking() && player.getVelocity().getY() < -0.5) break;

                    // Make sure that the player is not elytra flying
                    if (ClientUtils.hasElytraEquipt()) break;
                }
                
                // I believe this just says to the server "ay yo, I am on floor dw 'bout it sweet cheeks :3"
                player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
                
                break;
            }
        }
    }
}