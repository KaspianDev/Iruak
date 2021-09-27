package dev.brighten.anticheat.premium.impl.hitboxes;

import cc.funkemunky.api.tinyprotocol.api.ProtocolVersion;
import cc.funkemunky.api.tinyprotocol.packet.in.WrappedInFlyingPacket;
import cc.funkemunky.api.tinyprotocol.packet.in.WrappedInUseEntityPacket;
import cc.funkemunky.api.tinyprotocol.packet.out.WrappedOutEntityTeleportPacket;
import cc.funkemunky.api.tinyprotocol.packet.out.WrappedOutRelativePosition;
import cc.funkemunky.api.utils.KLocation;
import cc.funkemunky.api.utils.MathUtils;
import cc.funkemunky.api.utils.world.EntityData;
import cc.funkemunky.api.utils.world.types.SimpleCollisionBox;
import dev.brighten.anticheat.Kauri;
import dev.brighten.anticheat.check.api.Check;
import dev.brighten.anticheat.check.api.CheckInfo;
import dev.brighten.anticheat.check.api.Packet;
import dev.brighten.anticheat.data.ObjectData;
import dev.brighten.anticheat.utils.AxisAlignedBB;
import dev.brighten.anticheat.utils.EntityLocation;
import dev.brighten.anticheat.utils.MiscUtils;
import dev.brighten.anticheat.utils.Vec3D;
import dev.brighten.anticheat.utils.timer.Timer;
import dev.brighten.anticheat.utils.timer.impl.AtlasTimer;
import dev.brighten.anticheat.utils.timer.impl.PlayerTimer;
import dev.brighten.api.KauriVersion;
import dev.brighten.api.check.CheckType;
import lombok.val;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;

@CheckInfo(name = "Reach (B)", planVersion = KauriVersion.ARA, developer = true, checkType = CheckType.HITBOX)
public class ReachB extends Check {

    private EntityLocation eloc = new EntityLocation(UUID.randomUUID());
    private Timer lastFlying;
    private int streak;
    private float buffer;
    private boolean sentTeleport, attacked;

    @Override
    public void setData(ObjectData data) {
        super.setData(data);
        lastFlying = new PlayerTimer(data);
    }

    @Packet
    public void onUse(WrappedInUseEntityPacket packet) {
        if(data.target == null || packet.getAction() != WrappedInUseEntityPacket.EnumEntityUseAction.ATTACK)
            return;

        //Updating new entity loc
        if(data.target.getUniqueId() != eloc.uuid) {
            eloc = new EntityLocation(data.target.getUniqueId());
            sentTeleport = false;
        }

        attacked = true;
    }

    @Packet
    public void onFlying(WrappedInFlyingPacket packet) {
        if(attacked) {
            attacked = false;
            KLocation eyeLoc = data.playerInfo.to.clone(), eyeLocFrom = data.playerInfo.from.clone();

            eyeLoc.y+= data.playerInfo.sneaking ? 1.54f : 1.62f;
            eyeLocFrom.y+= data.playerInfo.lsneaking ? 1.54f : 1.62f;

            if(eloc.x == 0 && eloc.y == 0 & eloc.z == 0) return;

            SimpleCollisionBox targetBox = (SimpleCollisionBox) EntityData
                    .getEntityBox(new Vector(eloc.x, eloc.y, eloc.z), data.target);

            if(data.playerVersion.isBelow(ProtocolVersion.V1_9)) {
                targetBox = targetBox.expand(0.1, 0.1, 0.1);
            }

            AxisAlignedBB vanillaBox = new AxisAlignedBB(targetBox);

            val intersect = vanillaBox.rayTrace(eyeLoc.toVector(), MathUtils.getDirection(eyeLoc), 10);
            val intersect2 = vanillaBox
                    .rayTrace(eyeLocFrom.toVector(), MathUtils.getDirection(eyeLocFrom), 10);

            double distance = Double.MAX_VALUE;
            boolean collided = false; //Using this to compare smaller numbers than Double.MAX_VALUE. Slightly faster

            if(intersect != null) {
                distance = intersect.distanceSquared(new Vec3D(eyeLoc.x, eyeLoc.y, eyeLoc.z));
                collided = true;
            }

            if(intersect2 != null) {
                distance = Math.min(distance, intersect2
                        .distanceSquared(new Vec3D(eyeLocFrom.x, eyeLocFrom.y, eyeLocFrom.z)));
                collided = true;
            }

            if(collided) {
                distance = Math.sqrt(distance);
                if(distance > 3.02 && streak > 7 && sentTeleport && lastFlying.isNotPassed(2)) {
                    if(++buffer > 2) {
                        vl++;
                        flag("d=%.4f", distance);
                        buffer = 2;
                    }
                } else if(buffer > 0) buffer-= 0.02f;
                debug("dist=%.2f b=%s s=%s st=%s lf=%s", distance, buffer, streak, sentTeleport, lastFlying.getPassed());
            } else debug("didnt hit box: x=%.1f y=%.1f z=%.1f", eloc.x, eloc.y, eloc.z);
        }


        eloc.interpolateLocation();
        if(lastFlying.isNotPassed(2)) streak++;
        else {
            streak = 1;
            sentTeleport = false;
        }

        lastFlying.reset();
    }

    @Packet
    public void onEntity(WrappedOutRelativePosition packet, int now) {
        if(data.target != null && data.target.getEntityId() == packet.getId()) {
            data.runInstantAction(() -> {
                //We don't need to do version checking here. Atlas handles this for us.
                if(ProtocolVersion.getGameVersion().isBelow(ProtocolVersion.V1_9)) {
                    eloc.newX += (byte)packet.getX() / 32D;
                    eloc.newY += (byte)packet.getY() / 32D;
                    eloc.newZ += (byte)packet.getZ() / 32D;
                    eloc.newYaw += (float)(byte)packet.getYaw() / 256.0F * 360.0F;
                    eloc.newPitch += (float)(byte)packet.getPitch() / 256.0F * 360.0F;
                } else if(ProtocolVersion.getGameVersion().isBelow(ProtocolVersion.V1_14)) {
                    eloc.newX += (long)packet.getX() / 4096D;
                    eloc.newY += (long)packet.getY() / 4096D;
                    eloc.newZ += (long)packet.getZ() / 4096D;
                    eloc.newYaw += (float)(byte)packet.getYaw() / 256.0F * 360.0F;
                    eloc.newPitch += (float)(byte)packet.getPitch() / 256.0F * 360.0F;
                } else {
                    eloc.newX += (short)packet.getX() / 4096D;
                    eloc.newY += (short)packet.getY() / 4096D;
                    eloc.newZ += (short)packet.getZ() / 4096D;
                    eloc.newYaw += (float)(byte)packet.getYaw() / 256.0F * 360.0F;
                    eloc.newPitch += (float)(byte)packet.getPitch() / 256.0F * 360.0F;
                }

                eloc.increment = 3;
            });
        }
    }

    @Packet
    public void onTeleport(WrappedOutEntityTeleportPacket packet, int now) {
        if(data.target != null && data.target.getEntityId() == packet.entityId) {
            data.runInstantAction(() -> {
                if(data.playerVersion.isOrAbove(ProtocolVersion.V1_9)) {
                    eloc.increment = 0;
                    //We don't need to do version checking here. Atlas handles this for us.
                    eloc.newX = eloc.x = packet.x;
                    eloc.newY = eloc.y = packet.y;
                    eloc.newZ = eloc.z = packet.z;
                    eloc.newYaw = eloc.yaw = packet.yaw;
                    eloc.newPitch = eloc.pitch = packet.pitch;
                } else {
                    //We don't need to do version checking here. Atlas handles this for us.
                    eloc.newX = packet.x;
                    eloc.newY = packet.y;
                    eloc.newZ = packet.z;
                    eloc.newYaw = packet.yaw;
                    eloc.newPitch = packet.pitch ;

                    eloc.increment = 3;
                }

                sentTeleport = true;

                debug("teleport: %s", MiscUtils.currentTick() - now);
            });
        }
    }

}
