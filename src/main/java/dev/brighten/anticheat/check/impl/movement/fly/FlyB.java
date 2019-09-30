package dev.brighten.anticheat.check.impl.movement.fly;

import cc.funkemunky.api.tinyprotocol.packet.in.WrappedInFlyingPacket;
import dev.brighten.anticheat.check.api.Check;
import dev.brighten.anticheat.check.api.CheckInfo;
import dev.brighten.anticheat.check.api.CheckType;
import dev.brighten.anticheat.check.api.Packet;
import dev.brighten.anticheat.utils.MovementUtils;

@CheckInfo(name = "Fly (B)", description = "Ensures the player doesn't exceed max possible vertical motion.",
        checkType = CheckType.FLIGHT, punishVL = 5)
public class FlyB extends Check {

    @Packet
    public void onFlying(WrappedInFlyingPacket packet) {
        if(!packet.isPos()
                || data.playerInfo.flightCancel
                || data.playerInfo.halfBlockTicks > 0
                || data.playerInfo.lastVelocity.hasNotPassed(20)) return;

        float max = MovementUtils.getJumpHeight(data.getPlayer()) + 0.01f;

        if(data.playerInfo.deltaY > max) {
            if(vl++ > 1) {
                flag(data.playerInfo.deltaY + ">-" + max);
            }
        } else vl-= vl > 0 ? 0.02 : 0;
    }

}
