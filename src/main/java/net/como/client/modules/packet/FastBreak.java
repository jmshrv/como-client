package net.como.client.modules.packet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.como.client.ComoClient;
import net.como.client.events.ClientTickEvent;
import net.como.client.events.SendPacketEvent;
import net.como.client.events.UpdateBlockBreakingProgressEvent;
import net.como.client.structures.Module;
import net.como.client.structures.events.Event;
import net.como.client.structures.settings.Setting;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class FastBreak extends Module {
    public FastBreak() {
        super("FastBreak");

        this.description = "Allows you to break blocks a bit quicker.";

        this.addSetting(new Setting("PotionAmplifier", 3));
        this.addSetting(new Setting("Potion", true));
        // TODO change this when we have enum settings, this is horrible

        this.addSetting(new Setting("MultiplierOnly", false));

        this.addSetting(new Setting("BreakDelay", 0d));

        // The break multiplier could be used in conjunction with the other modes
        this.addSetting(new Setting("BreakMultiplier", 2));

        this.setCategory("Packet");
    }

    @Override
    public String listOption() {
        if (this.getBoolSetting("MultiplierOnly")) return String.format("x%d", this.getIntSetting("BreakMultiplier"));

        return this.getBoolSetting("Potion") ? "Potion" : "Packet";
    }

    public void resetPotionEffect() {
        if (ComoClient.me().hasStatusEffect(StatusEffects.HASTE)) {
            ComoClient.me().removeStatusEffect(StatusEffects.HASTE);
        }
    }

    @Override
    public void activate() {
        this.addListen(ClientTickEvent.class);
        this.addListen(SendPacketEvent.class);
        this.addListen(UpdateBlockBreakingProgressEvent.class);

        this.ignoreRequests = 0;
    }

    @Override
    public void deactivate() {
        this.removeListen(ClientTickEvent.class);
        this.removeListen(SendPacketEvent.class);
        this.removeListen(UpdateBlockBreakingProgressEvent.class);

        this.resetPotionEffect();
        this.targetBlocks.clear();
    }

    private static class TimedBreak {
        private BlockPos block;
        public Double breakTime;
        private Direction direction;

        public BlockPos getBlockPos() {return this.block;};

        public boolean doBreak() {
            if (ComoClient.getCurrentTime() < this.breakTime) return false;

            ComoClient.me().networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, this.block, direction));

            return true;
        }

        TimedBreak(BlockPos pos, Direction direction, Double delay) {
            this.block = pos;
            this.direction = direction;

            this.breakTime = ComoClient.getCurrentTime() + delay;
        }
    }

    private Boolean addBlock(PlayerActionC2SPacket packet) {
        if (this.targetBlocks.containsKey(packet.getPos())) return false;

        this.targetBlocks.put(packet.getPos(), new TimedBreak(packet.getPos(), packet.getDirection(), this.getDoubleSetting("BreakDelay")));
        return true;
    }

    private Boolean removeBlock(BlockPos pos) {
        if (!this.targetBlocks.containsKey(pos)) return false;

        this.targetBlocks.remove(pos);

        return true;
    }

    private boolean handlePotions() {
        if (!this.getBoolSetting("Potion")) return false;

        ComoClient.me().addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 3, this.getIntSetting("PotionAmplifier"), true, true));
        return true;
    }

    private boolean handlePackets() {
        if (this.getBoolSetting("Potion")) return false;

        // Remove the potion effect since we are no longer in that mode.
        this.resetPotionEffect();

        // Positions that we need to remove from the hashmap (we cannot remove during iteration over the hashmap.)
        List<BlockPos> removedPositions = new ArrayList<BlockPos>();

        // Do all the block breaking
        for (TimedBreak timedBreak : targetBlocks.values()) {
            if (timedBreak.doBreak()) {
                removedPositions.add(timedBreak.getBlockPos());
            }
        }

        // Remove the blocks from the array.
        for (BlockPos pos : removedPositions) {
            this.removeBlock(pos);
        }

        return true;
    }

    private HashMap<BlockPos, TimedBreak> targetBlocks = new HashMap<>(); 
    private Integer ignoreRequests = 0;

    @Override
    public void fireEvent(Event event) {
        switch (event.getClass().getSimpleName()) {
            case "ClientTickEvent": {
                // Stop if we only have multipliers
                if (this.getBoolSetting("MultiplierOnly")) {
                    this.resetPotionEffect();
                    break;
                }

                // Handle simple potion effect.
                if (this.handlePotions()) break;
                
                // Handle packets
                if (this.handlePackets()) break;

                break;
            }

            case "SendPacketEvent": {
                SendPacketEvent e = (SendPacketEvent)event;

                if (!(e.packet instanceof PlayerActionC2SPacket)) break;

                PlayerActionC2SPacket packet = (PlayerActionC2SPacket)e.packet;

                // Handle mining a block
                switch (packet.getAction()) {
                    case ABORT_DESTROY_BLOCK:
                    case STOP_DESTROY_BLOCK: {
                        this.removeBlock(packet.getPos());
                        break;
                    }

                    case START_DESTROY_BLOCK: {
                        this.addBlock(packet);

                        break;
                    }

                    default: {
                        break;
                    }
                }

                break;
            }

            case "UpdateBlockBreakingProgressEvent": {
                UpdateBlockBreakingProgressEvent e = (UpdateBlockBreakingProgressEvent)event;
                
                Integer multiplier = this.getIntSetting("BreakMultiplier");
                if (multiplier <= 1) break; // Handle if there is no change. 

                // Don't block anything that we spawned.
                if (ignoreRequests > 0) {
                    ignoreRequests--;
                    break;
                }

                // Cancel the current call.
                e.cir.cancel();

                // Add the newly called functions
                this.ignoreRequests += multiplier;

                // Call them
                for (int i = 0; i < multiplier; i++) {
                    ComoClient.getClient().interactionManager.updateBlockBreakingProgress(e.pos, e.direction);
                }
                
                break;
            }
        }
    }
}