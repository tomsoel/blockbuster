package mchorse.blockbuster.common.entity;

import com.mojang.authlib.GameProfile;

import io.netty.buffer.ByteBuf;
import mchorse.blockbuster.Blockbuster;
import mchorse.blockbuster.api.Model;
import mchorse.blockbuster.api.ModelHandler;
import mchorse.blockbuster.common.ClientProxy;
import mchorse.blockbuster.common.CommonProxy;
import mchorse.blockbuster.common.GuiHandler;
import mchorse.blockbuster.common.item.ItemActorConfig;
import mchorse.blockbuster.common.item.ItemRegister;
import mchorse.blockbuster.common.tileentity.TileEntityDirector;
import mchorse.blockbuster.network.Dispatcher;
import mchorse.blockbuster.network.common.PacketModifyActor;
import mchorse.blockbuster.network.common.recording.PacketSyncTick;
import mchorse.blockbuster.recording.RecordPlayer;
import mchorse.blockbuster.recording.Utils;
import mchorse.blockbuster.recording.data.Mode;
import mchorse.blockbuster.utils.EntityUtils;
import mchorse.blockbuster.utils.L10n;
import mchorse.blockbuster.utils.NBTUtils;
import mchorse.blockbuster.utils.RLUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityBodyHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

/**
 * Actor entity class
 *
 * Actor entity class is responsible for recording player's actions and execute
 * them. I'm also thinking about giving them controllable AI settings so they
 * could be used without recording (like during the battles between two or more
 * actors).
 *
 * Also, it would be cool to add something like simple crowd control for bigger
 * scenes (like one from Van Helsing in beginning with big crowd with torches,
 * fire and stuff).
 */
public class EntityActor extends EntityLiving implements IEntityAdditionalSpawnData
{
    /**
     * Skin used by the actor. If empty - means default skin provided with this
     * mod.
     */
    public ResourceLocation skin;

    /**
     * Model which is used to display. If empty - means default model (steve)
     * provided with this mod.
     */
    public String model = "";

    /**
     * Model instance, used for setting the size of this entity in updateSize
     * method
     */
    private Model modelInstance;

    /**
     * Position of director's block (needed to start the playback of other
     * actors while recording this actor).
     */
    public BlockPos directorBlock;

    /**
     * Temporary solution for disallowing rendering of custom name tag in GUI.
     */
    public boolean renderName = true;

    /**
     * This field is needed to make actors invisible. This is helpful for
     * scenes with different characters, which isn't needed to be seen.
     */
    public boolean invisible = false;

    /**
     * Fake player used in some of methods like onBlockActivated to avoid
     * NullPointerException (and some math like the direction in which to open
     * the fence or something).
     */
    public EntityPlayer fakePlayer;

    /**
     * In Soviet Russia, playback plays you
     */
    public RecordPlayer playback;

    /**
     * Backward compatibility filename thing
     */
    public String _filename = "";

    /* Default pose sizes */
    private float[] flying = {0.6F, 0.6F};
    private float[] sneaking = {0.6F, 1.65F};
    private float[] standing = {0.6F, 1.80F};

    public EntityActor(World worldIn)
    {
        super(worldIn);

        this.fakePlayer = new EntityPlayer(worldIn, new GameProfile(null, "xXx_Fake_Player_420_xXx"))
        {
            @Override
            public boolean isSpectator()
            {
                return false;
            }

            @Override
            public boolean isCreative()
            {
                return false;
            }
        };
    }

    /**
     * Check whether this actor is playing
     */
    public boolean isPlaying()
    {
        return this.playback != null && !this.playback.isFinished();
    }

    /**
     * Returns the Y Offset of this entity.
     *
     * Taken from EntityPlayer.
     */
    @Override
    public double getYOffset()
    {
        return -0.35D;
    }

    /**
     * Can't despawn an actor
     */
    @Override
    protected boolean canDespawn()
    {
        return false;
    }

    /**
     * Brutally stolen from EntityPlayer class
     */
    public void setElytraFlying(boolean isFlying)
    {
        this.setFlag(7, isFlying);
    }

    /**
     * This is also brutally stolen from EntityPlayer class, by the way, I don't
     * think that changing the height while sneaking can save player's life
     */
    protected void updateSize()
    {
        float[] pose;

        if (this.modelInstance != null)
        {
            pose = this.modelInstance.getPose(EntityUtils.poseForEntity(this)).size;
        }
        else
        {
            pose = this.isElytraFlying() ? this.flying : (this.isSneaking() ? this.sneaking : this.standing);
        }

        this.setSize(pose[0], pose[1]);
    }

    /**
     * Adjust the movement, limb swinging, and process action stuff.
     *
     * See process actions method for more information.
     */
    @Override
    public void onLivingUpdate()
    {
        this.updateSize();
        this.pickUpNearByItems();

        if (this.playback != null && this.playback.playing)
        {
            this.playback.next(this);

            if (!this.worldObj.isRemote)
            {
                int tick = this.playback.tick;

                if (this.playback.isFinished())
                {
                    CommonProxy.manager.stopPlayback(this);
                }
                else if (tick != 0 && tick % Blockbuster.proxy.config.record_sync_rate == 0)
                {
                    Dispatcher.sendToTracked(this, new PacketSyncTick(this.getEntityId(), tick));
                }
            }
        }

        /* Copy paste of onLivingUpdate from EntityLivingBase, I believe */
        this.updateArmSwingProgress();

        if (this.worldObj.isRemote && this.newPosRotationIncrements > 0)
        {
            double d0 = this.posX + (this.interpTargetX - this.posX) / this.newPosRotationIncrements;
            double d1 = this.posY + (this.interpTargetY - this.posY) / this.newPosRotationIncrements;
            double d2 = this.posZ + (this.interpTargetZ - this.posZ) / this.newPosRotationIncrements;

            this.newPosRotationIncrements--;
            this.setPosition(d0, d1, d2);
        }
        else if (!this.isServerWorld())
        {
            this.motionX *= 0.98D;
            this.motionY *= 0.98D;
            this.motionZ *= 0.98D;
        }

        if (Math.abs(this.motionX) < 0.005D) this.motionX = 0.0D;
        if (Math.abs(this.motionY) < 0.005D) this.motionY = 0.0D;
        if (Math.abs(this.motionZ) < 0.005D) this.motionZ = 0.0D;

        /* Trigger pressure playback */
        this.moveEntityWithHeading(this.moveStrafing, this.moveForward);
    }

    /**
     * Update fall state.
     *
     * This override is responsible for applying fall damage on the actor.
     * {@link #moveEntity(double, double, double)} seem to override onGround
     * property wrongly on the server, so we have to do this bullshit.
     */
    @Override
    protected void updateFallState(double y, boolean onGroundIn, IBlockState state, BlockPos pos)
    {
        if (!this.worldObj.isRemote && Blockbuster.proxy.config.actor_fall_damage && this.playback != null)
        {
            /* Override onGround field */
            this.onGround = onGroundIn = this.playback.record.frames.get(this.playback.tick).onGround;
        }

        super.updateFallState(y, onGroundIn, state, pos);
    }

    /**
     * Destroy near by items
     *
     * Taken from super implementation of onLivingUpdate. You can't use
     * super.onLivingUpdate() in onLivingUpdate(), because it will distort
     * actor's movement (make it more laggy)
     */
    private void pickUpNearByItems()
    {
        if (!this.worldObj.isRemote && !this.dead)
        {
            for (EntityItem entityitem : this.worldObj.getEntitiesWithinAABB(EntityItem.class, this.getEntityBoundingBox().expand(1.0D, 0.0D, 1.0D)))
            {
                if (!entityitem.isDead && entityitem.getEntityItem() != null && !entityitem.cannotPickup())
                {
                    entityitem.setDead();
                }
            }
        }
    }

    /**
     * Roll back to {@link EntityLivingBase}'s updateDistance methods.
     *
     * Its implementation supports much superior renderYawOffset animation.
     * Well, at least that's what I think. I should check out
     * {@link EntityBodyHelper} before making final decision.
    */
    @Override
    protected float updateDistance(float f2, float f3)
    {
        float f = MathHelper.wrapDegrees(f2 - this.renderYawOffset);
        this.renderYawOffset += f * 0.3F;
        float f1 = MathHelper.wrapDegrees(this.rotationYaw - this.renderYawOffset);
        boolean flag = f1 < -90.0F || f1 >= 90.0F;

        if (f1 < -75.0F)
        {
            f1 = -75.0F;
        }

        if (f1 >= 75.0F)
        {
            f1 = 75.0F;
        }

        this.renderYawOffset = this.rotationYaw - f1;

        if (f1 * f1 > 2500.0F)
        {
            this.renderYawOffset += f1 * 0.2F;
        }

        if (flag)
        {
            f3 *= -1.0F;
        }

        return f3;
    }

    /* Processing interaction with player */

    /**
     * Process interact
     *
     * Inject UUID of actor to registering device, open GUI for changing actor's
     * skin, or start recording him
     */
    @Override
    protected boolean processInteract(EntityPlayer player, EnumHand p_184645_2_, ItemStack stack)
    {
        ItemStack item = player.getHeldItemMainhand();

        if (item != null && (this.handleRegisterItem(item, player) || this.handleSkinItem(item, player)))
        {
            return true;
        }
        else if (item == null)
        {
            if (!this.worldObj.isRemote) this.startRecording(player);

            return true;
        }

        return false;
    }

    /**
     * Set actor's id on register item (while using register item on this actor)
     */
    private boolean handleRegisterItem(ItemStack stack, EntityPlayer player)
    {
        boolean holdsRegisterItem = stack.getItem() instanceof ItemRegister;

        if (!this.worldObj.isRemote && holdsRegisterItem)
        {
            ItemRegister item = (ItemRegister) stack.getItem();
            BlockPos pos = item.getBlockPos(stack);

            if (pos == null)
            {
                L10n.error(player, "actor.not_attached");

                return false;
            }

            TileEntity tile = this.worldObj.getTileEntity(pos);

            if (tile != null && tile instanceof TileEntityDirector)
            {
                TileEntityDirector director = (TileEntityDirector) tile;

                if (!director.add(this))
                {
                    L10n.info(player, "director.already_registered");
                }
                else
                {
                    L10n.success(player, "director.was_registered");
                }
            }
            else
            {
                L10n.error(player, "director.missing", pos.getX(), pos.getY(), pos.getZ());
            }
        }

        return holdsRegisterItem;
    }

    /**
     * Open skin choosing GUI by using skin managing item
     */
    private boolean handleSkinItem(ItemStack stack, EntityPlayer player)
    {
        boolean holdsSkinItem = stack.getItem() instanceof ItemActorConfig;

        if (this.worldObj.isRemote && holdsSkinItem)
        {
            GuiHandler.open(player, GuiHandler.ACTOR, this.getEntityId(), 0, 0);
        }

        return holdsSkinItem;
    }

    /* Public API */

    /**
     * Start the playback, invoked by director block (more specifically by
     * DirectorTileEntity).
     */
    public void startPlaying(String filename, boolean kill)
    {
        if (CommonProxy.manager.players.containsKey(this))
        {
            Utils.broadcastMessage("blockbuster.info.actor.playing", new Object[] {});

            return;
        }

        CommonProxy.manager.startPlayback(filename, this, Mode.BOTH, kill, true);
    }

    /**
     * Stop playing
     */
    public void stopPlaying()
    {
        CommonProxy.manager.stopPlayback(this);
    }

    /**
     * Start recording the player's actions for this actor
     */
    private void startRecording(EntityPlayer player)
    {
        if (this.directorBlock == null) return;

        TileEntity tile = player.worldObj.getTileEntity(this.directorBlock);

        if (tile != null && tile instanceof TileEntityDirector)
        {
            TileEntityDirector director = (TileEntityDirector) tile;

            if (!CommonProxy.manager.recorders.containsKey(player))
            {
                director.startPlayback(this);
            }
            else
            {
                director.stopPlayback(this);
            }

            director.startRecording(this, player);
        }
    }

    /**
     * Configure this actor
     *
     * Takes four properties to modify: filename used as id for recording,
     * displayed name, rendering skin and invulnerability flag
     */
    public void modify(String model, ResourceLocation skin, boolean invisible, boolean notify)
    {
        this.model = model;
        this.skin = skin;
        this.invisible = invisible;

        this.updateModel();

        if (!this.worldObj.isRemote && notify)
        {
            this.notifyPlayers();
        }
    }

    /**
     * Update the data model
     */
    private void updateModel()
    {
        ModelHandler models = Blockbuster.proxy.models;

        if (models.models.containsKey(this.model))
        {
            this.modelInstance = models.models.get(this.model);
        }
    }

    /**
     * Notify trackers of data changes happened in this actor
     */
    public void notifyPlayers()
    {
        Dispatcher.sendToTracked(this, new PacketModifyActor(this.getEntityId(), this.model, this.skin, this.invisible));
    }

    /* Reading/writing to disk */

    @Override
    public void readEntityFromNBT(NBTTagCompound tag)
    {
        super.readEntityFromNBT(tag);

        this.model = tag.getString("Model");
        this.skin = RLUtils.fromString(tag.getString("Skin"), this.model.isEmpty() ? "steve" : "");
        this.invisible = tag.getBoolean("Invisible");

        this.directorBlock = NBTUtils.getBlockPos("Dir", tag);
        this._filename = tag.getString("Filename");

        if (!this.worldObj.isRemote)
        {
            this.notifyPlayers();
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag)
    {
        super.writeEntityToNBT(tag);

        if (this.skin != null)
        {
            tag.setString("Skin", this.skin.toString());
        }

        if (!this.model.isEmpty())
        {
            tag.setString("Model", this.model);
        }

        if (this.directorBlock != null)
        {
            NBTUtils.saveBlockPos("Dir", tag, this.directorBlock);
        }

        tag.setBoolean("Invisible", this.invisible);
    }

    /* IEntityAdditionalSpawnData implementation */

    @Override
    public void writeSpawnData(ByteBuf buffer)
    {
        ByteBufUtils.writeUTF8String(buffer, this.model);
        ByteBufUtils.writeUTF8String(buffer, this.skin == null ? "" : this.skin.toString());
        buffer.writeBoolean(this.invisible);
        buffer.writeBoolean(this.isPlaying());

        if (this.isPlaying())
        {
            buffer.writeInt(this.playback.tick);
            buffer.writeByte(this.playback.recordDelay);
            ByteBufUtils.writeUTF8String(buffer, this.playback.record.filename);
        }

        /* What a shame, Mojang, why do I need to synchronize your shit?! */
        buffer.writeBoolean(this.isEntityInvulnerable(DamageSource.anvil));
    }

    @Override
    public void readSpawnData(ByteBuf buffer)
    {
        this.model = ByteBufUtils.readUTF8String(buffer);
        this.skin = RLUtils.fromString(ByteBufUtils.readUTF8String(buffer), this.model);
        this.invisible = buffer.readBoolean();

        if (buffer.readBoolean())
        {
            int tick = buffer.readInt();
            int delay = buffer.readByte();
            String filename = ByteBufUtils.readUTF8String(buffer);

            if (this.playback == null)
            {
                if (ClientProxy.manager.records.containsKey(filename))
                {
                    this.playback = new RecordPlayer(ClientProxy.manager.records.get(filename), Mode.FRAMES);
                }
                else
                {
                    this.playback = new RecordPlayer(null, Mode.FRAMES);
                }
            }

            if (this.playback != null)
            {
                this.playback.tick = tick;
                this.playback.recordDelay = delay;
            }
        }

        this.setEntityInvulnerable(buffer.readBoolean());
    }

    public void setItemStackInUse(int activeCount)
    {
        this.activeItemStackUseCount = activeCount;
    }
}