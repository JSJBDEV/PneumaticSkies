package ace.actually.pneuskies.mixin;

import me.desht.pneumaticcraft.common.drone.EntityPathNavigateDrone;
import me.desht.pneumaticcraft.common.drone.ai.DroneAIBlockInteraction;
import me.desht.pneumaticcraft.common.entity.drone.DroneEntity;
import me.desht.pneumaticcraft.common.network.LocationDoublePacket;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketSetEntityMotion;
import me.desht.pneumaticcraft.common.network.PacketSpawnParticleTrail;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import javax.annotation.Nullable;

@Mixin(targets = "me.desht.pneumaticcraft.common.drone.EntityPathNavigateDrone", remap = false)
@Pseudo
public abstract class EntityPathNavDroneMixin extends PathNavigation {
    @Shadow @Final private DroneEntity droneEntity;

    @Shadow private BlockPos telPos;

    public EntityPathNavDroneMixin(Mob p_26515_, Level p_26516_) {
        super(p_26515_, p_26516_);
    }

    @Shadow public abstract boolean isDone();

    @Shadow public abstract void tick();

    @Shadow @Nullable public abstract Path createPath(BlockPos pos, int p2);

    @Inject(method = "teleport", at = @At("HEAD"), cancellable = true)
    private void teleport(CallbackInfo ci)
    {
        Vec3 dest = VSGameUtilsKt.toWorldCoordinates(droneEntity.world(),Vec3.atCenterOf(this.telPos));
        NetworkHandler.sendToAllTracking(new PacketSpawnParticleTrail(ParticleTypes.PORTAL, this.droneEntity.getX(), this.droneEntity.getY(), this.droneEntity.getZ(), dest.x, dest.y, dest.z), this.droneEntity);
        this.droneEntity.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        this.droneEntity.setPos(dest.x, dest.y, dest.z);
        ci.cancel();
    }

    /**
     * I didn't look too much into refmaps, so you may need to change the method= to m_7864_ in a non-dev env
     * in this method we tell the drone that it is pathing to real world blocks
     * if the drone occupies the block above its inventory, it teleports to the shipyard so it can actually interact with it
     * this is a very hacky workaround, but if you look at the pneumatic code you will find this is non-trivial to do properly.
     * @param pos
     * @return
     */
    @ModifyVariable(method = "createPath(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("HEAD"), ordinal = 0)
    private BlockPos path(BlockPos pos)
    {
        BlockPos realWorldPos = BlockPos.containing(VSGameUtilsKt.toWorldCoordinates(droneEntity.world(),Vec3.atCenterOf(pos)));
        if(BlockPos.containing(droneEntity.position()).equals(realWorldPos))
        {

            this.droneEntity.setPos(pos.getX(),pos.getY(),pos.getZ());

            this.droneEntity.addTag("flickering");
            return pos;
        }
        return realWorldPos;
    }

    /**
     * brings the drone back to worldspace after a tick using minecrafts tags :clueless:
     * you might need to change this to m_7638_ if you want to use this in a non dev env
     * TODO: make the drone actually visible when it returns (why does it not show up? .teleportTo stops it functioning properly...)
     * TODO: work out why the drone seems to end up some distance away when it comes back
     * @param ci
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci)
    {
        if(this.droneEntity.getTags().contains("flickering"))
        {
            this.droneEntity.removeTag("flickering");
            this.droneEntity.addTag("returning");
        }
        else if(this.droneEntity.getTags().contains("returning"))
        {
            this.droneEntity.removeTag("returning");
            Vec3 v3 = VSGameUtilsKt.toWorldCoordinates(droneEntity.world(),Vec3.atCenterOf(droneEntity.getDeployPos()));
            System.out.println(v3);
            this.droneEntity.setPos(v3.x,v3.y+1,v3.z);
            System.out.println(this.droneEntity.getOnPos());
        }
    }
}
