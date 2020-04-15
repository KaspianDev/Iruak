package dev.brighten.anticheat.check.impl.combat.hitbox;

import cc.funkemunky.api.tinyprotocol.packet.in.WrappedInFlyingPacket;
import cc.funkemunky.api.utils.KLocation;
import cc.funkemunky.api.utils.MathUtils;
import cc.funkemunky.api.utils.MiscUtils;
import cc.funkemunky.api.utils.world.types.RayCollision;
import cc.funkemunky.api.utils.world.types.SimpleCollisionBox;
import dev.brighten.anticheat.Kauri;
import dev.brighten.anticheat.check.api.*;
import dev.brighten.anticheat.data.ObjectData;
import dev.brighten.anticheat.utils.AtomicDouble;
import dev.brighten.api.check.CheckType;
import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@CheckInfo(name = "Hitboxes", description = "Checks if the player attacks outside a player's hitbox.",
        checkType = CheckType.HITBOX, punishVL = 15)
@Cancellable(cancelType = CancelType.ATTACK)
public class Hitboxes extends Check {

    private static List<EntityType> allowedEntities = Arrays.asList(
            EntityType.ZOMBIE,
            EntityType.VILLAGER,
            EntityType.PLAYER,
            EntityType.SKELETON,
            EntityType.PIG_ZOMBIE,
            EntityType.WITCH,
            EntityType.CREEPER,
            EntityType.ENDERMAN);

    @Packet
    public void onFlying(WrappedInFlyingPacket packet, long timeStamp) {
        if (checkParameters(data)) {

            List<RayCollision> rayTrace = data.pastLocation.getEstimatedLocation(timeStamp,
                    0, 100L + (data.lagInfo.transPing / 2)).stream()
                    .map(l -> {
                        KLocation loc = l.clone();
                        loc.y+=data.playerInfo.sneaking ? 1.54 : 1.62;
                        return new RayCollision(loc.toVector(),
                                MathUtils.getDirection(loc));
                    })
                    .collect(Collectors.toList());

            List<SimpleCollisionBox> entityLocations = data.targetPastLocation
                    .getEstimatedLocation(timeStamp,
                            data.lagInfo.transPing, 150L
                                    + Math.abs(data.lagInfo.transPing - data.lagInfo.lastTransPing))
                    .stream()
                    .map(loc -> getHitbox(loc, data.target.getType()))
                    .collect(Collectors.toList());

            long collisions = 0;
            AtomicDouble distance = new AtomicDouble(10);

            for (RayCollision ray : rayTrace) {
                collisions+= entityLocations.stream().filter(bb -> {
                    Vector point;
                    if((point = ray.collisionPoint(bb)) != null) {
                        double dist = point.distance(ray.getOrigin());

                        distance.set(Math.min(dist, distance.get()));
                        return dist < 3.4f;
                    }
                    return false;
                }).count();
            }

            if (collisions == 0
                    && timeStamp - data.creation > 3000L
                    && data.lagInfo.lastPacketDrop.hasPassed(4)) {
                if(vl++ > 10)  flag("distance=%v ping=%p tps=%t",
                        distance.get() != -1 ? distance.get() : "[none collided]");
            } else vl -= vl > 0 ? 0.5 : 0;

            debug("collided=" + collisions + " distance=" + distance.get() + " type=" + data.target.getType());
        }
    }

    private static boolean checkParameters(ObjectData data) {
        return data.playerInfo.lastAttack.hasNotPassed(0)
                && data.target != null
                && data.target.getType().equals(EntityType.PLAYER) && ((Player) data.target).isOnline()
                && data.targetPastLocation.previousLocations.size() > 12
                && Kauri.INSTANCE.lastTickLag.hasPassed(10)
                && allowedEntities.contains(data.target.getType())
                && !data.playerInfo.creative
                && data.playerInfo.lastTargetSwitch.hasPassed()
                && !data.getPlayer().getGameMode().equals(GameMode.CREATIVE);
    }

    private static SimpleCollisionBox getHitbox(KLocation loc, EntityType type) {
        if(type.equals(EntityType.PLAYER)) {
            return new SimpleCollisionBox(loc.toVector(), 0.6, 1.8).expand(0.25, 0.25, 0.25);
        } else {
            Vector bounds = MiscUtils.entityDimensions.get(type);

            return new SimpleCollisionBox(loc.toVector(), bounds.getX() + bounds.getZ(), bounds.getY())
            .expand(0.3, 0.3, 0.3);
        }
    }
}