package net.como.client.modules;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.como.client.CheatClient;
import net.como.client.components.ServerClientRotation;
import net.como.client.events.ClientTickEvent;
import net.como.client.events.OnRenderEvent;
import net.como.client.events.RenderWorldViewBobbingEvent;
import net.como.client.structures.Cheat;
import net.como.client.structures.events.Event;
import net.como.client.structures.settings.Setting;
import net.como.client.utils.ClientUtils;
import net.como.client.utils.MathsUtils;
import net.como.client.utils.RenderUtils;
import net.como.client.utils.RotationUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class KillAura extends Cheat {

    public KillAura() {
        super("KillAura");

        this.addSetting(new Setting("MaxDistance", 7d));
        this.addSetting(new Setting("Delay", 0d));
        this.addSetting(new Setting("SilentAim", true));

        this.addSetting(new Setting("AttackFriends", false));
        this.addSetting(new Setting("TargetClosestAngle", true));
        this.addSetting(new Setting("MaxFOV", 25d));

        this.addSetting(new Setting("TargetTracers", true));
        this.addSetting(new Setting("TracerLifeSpan", 0.25d));
    
        this.description = "Automatically attacks specified targets.";
    }
    
    private ServerClientRotation scRot = new ServerClientRotation();
    private Double nextValidTime = 0d;

    @Override
    public String listOption() {
        return (boolean)this.getSetting("SilentAim").value ? "Silent" : null;
    }

    @Override
    public void activate() {
        this.addListen(ClientTickEvent.class);
        this.addListen(RenderWorldViewBobbingEvent.class);
        this.addListen(OnRenderEvent.class);

        scRot.addListeners(this);
    }

    @Override
    public void deactivate() {
        this.removeListen(ClientTickEvent.class);
        this.removeListen(RenderWorldViewBobbingEvent.class);
        this.removeListen(OnRenderEvent.class);

        scRot.removeListeners(this);
    }

    private double getLookDistance(Entity entity) {
        return RotationUtils.getRequiredRotation( entity.getEyePos() ).difference( ClientUtils.getRotation() ).magnitude();
    }

    private Stream<Entity> applyFilters(Stream<Entity> stream) {

        return stream
            // Basics
            .filter(e -> e instanceof LivingEntity)
            .filter(e -> !e.isRemoved())

            // Make sure we are not gonna crystal ourselves.
            .filter(e -> !(e instanceof EndCrystalEntity))

            // Make sure that they are not over our max distance
            .filter(e -> !(e.distanceTo(CheatClient.me()) > (double)this.getSetting("MaxDistance").value))

            // Make sure they are not out of the FOV range
            .filter(e -> !(this.getLookDistance(e) > (double)this.getSetting("MaxFOV").value))

            // Make sure that they are alive
            .filter(e -> e.isAlive())

            // Make sure that we are not attacking friends where needed
            .filter(e -> !(e instanceof PlayerEntity) || !(CheatClient.friendsManager.onFriendList((PlayerEntity)e)) || (boolean)this.getSetting("AttackFriends").value )
            
            // Make sure it ain't us
            .filter(e -> e != CheatClient.me());
    }

    private Comparator<Entity> getComparator() {
        // Distance to localplayer

        if ((boolean)this.getSetting("TargetClosestAngle").value) {
            return new Comparator<Entity>() {
                public int compare(Entity e1, Entity e2) {
                    return Double.compare(getLookDistance(e1), getLookDistance(e2));
                }
            };
        }

        return new Comparator<Entity>() {
            public int compare(Entity e1, Entity e2) {
                return Double.compare(e1.distanceTo(CheatClient.me()), e2.distanceTo(CheatClient.me()));
            }
        };
    }

    private HashMap<Entity, Double> prevTargets = new HashMap<Entity, Double>();

    private boolean shouldDoAttack() {
        // idk what the baseTime does
        return
                CheatClient.me().getAttackCooldownProgress(0) == 1.0f
                && CheatClient.getCurrentTime() >= this.nextValidTime;
    }

    @Override
    public void fireEvent(Event event) {
        if (scRot.fireEvent(event)) return;

        switch (event.getClass().getSimpleName()) {
            case "ClientTickEvent": {
                if (!this.shouldDoAttack()) break;

                Stream<Entity> stream = this.applyFilters(
                    StreamSupport.stream(
                        CheatClient.getClient().world.getEntities().spliterator(),
                        true
                    )
                );

                Entity target = stream.min(this.getComparator()).orElse(null);
                if (target == null) break;

                // Make sure they are not too far
                Double distance = target.getPos().distanceTo(CheatClient.me().getPos());
                if (distance > (double)this.getSetting("MaxDistance").value) break;

                Vec3d targetPos = target.getEyePos();
                // Look at the entity
                if ((boolean)this.getSetting("SilentAim").value) {
                    scRot.lookAtPosServer(targetPos);
                } else {
                    scRot.lookAtPosClient(targetPos);
                }
                
                ClientUtils.hitEntity(target);

                // Get the next time
                this.nextValidTime = CheatClient.getCurrentTime() + (Double)this.getSetting("Delay").value;
                
                // For tracers
                if ((boolean)this.getSetting("TargetTracers").value)
                    this.prevTargets.put(target, CheatClient.getCurrentTime());

                break;
            }
            case "OnRenderEvent": {
                if (!(boolean)this.getSetting("TargetTracers").value) break;

                OnRenderEvent e = (OnRenderEvent)event;

                for (Iterator<HashMap.Entry<Entity, Double>> it = this.prevTargets.entrySet().iterator(); it.hasNext();) {
                    HashMap.Entry<Entity, Double> pair = it.next();

                    double hitTime = pair.getValue();

                    // Make sure that they are not out of date.
                    Double span = (Double)this.getSetting("TracerLifeSpan").value;
                    if (span > 0 && CheatClient.getCurrentTime() - hitTime > span) {
                        it.remove();
                        continue;
                    }

                    Entity ent = pair.getKey();
                    if (ent == null || !ent.isAlive()) {
                        it.remove();
                    }

                    RenderUtils.drawTracer(e.mStack, MathsUtils.getLerpedCentre(pair.getKey(), e.tickDelta), e.tickDelta);
                }

                break;
            }

            case "RenderWorldViewBobbingEvent": {
                RenderWorldViewBobbingEvent e = (RenderWorldViewBobbingEvent)event;
                if ((Boolean)this.getSetting("TargetTracers").value && this.prevTargets.size() > 0) {
                    e.cancel = true;
                }

                break;
            }
        }
    }
}