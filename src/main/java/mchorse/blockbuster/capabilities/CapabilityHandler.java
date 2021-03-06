package mchorse.blockbuster.capabilities;

import mchorse.blockbuster.Blockbuster;
import mchorse.blockbuster.camera.CameraUtils;
import mchorse.blockbuster.capabilities.morphing.IMorphing;
import mchorse.blockbuster.capabilities.morphing.Morphing;
import mchorse.blockbuster.capabilities.morphing.MorphingProvider;
import mchorse.blockbuster.capabilities.recording.IRecording;
import mchorse.blockbuster.capabilities.recording.Recording;
import mchorse.blockbuster.capabilities.recording.RecordingProvider;
import mchorse.blockbuster.common.entity.EntityActor;
import mchorse.blockbuster.network.Dispatcher;
import mchorse.blockbuster.network.common.PacketMorph;
import mchorse.blockbuster.network.common.PacketMorphPlayer;
import mchorse.blockbuster.recording.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

/**
 * Capability handler class
 *
 * This class is responsible for managing capabilities, i.e. attaching
 * capabilities and syncing values on the client.
 */
public class CapabilityHandler
{
    public static final ResourceLocation MORPHING_CAP = new ResourceLocation(Blockbuster.MODID, "morphing_capability");
    public static final ResourceLocation RECORDING_CAP = new ResourceLocation(Blockbuster.MODID, "recording_capability");

    /**
     * Attach capabilities (well, only one, right now)
     */
    @SubscribeEvent
    public void attachCapability(AttachCapabilitiesEvent.Entity event)
    {
        if (!(event.getEntity() instanceof EntityPlayer)) return;

        event.addCapability(MORPHING_CAP, new MorphingProvider());
        event.addCapability(RECORDING_CAP, new RecordingProvider());
    }

    /**
     * When player logs in, sent him his server counter partner's values.
     */
    @SubscribeEvent
    public void playerLogsIn(PlayerLoggedInEvent event)
    {
        EntityPlayer player = event.player;
        IMorphing capability = Morphing.get(player);
        IRecording recording = Recording.get(player);

        if (capability != null)
        {
            Dispatcher.sendTo(new PacketMorph(capability.getModel(), capability.getSkin()), (EntityPlayerMP) player);
        }

        if (recording != null && recording.hasProfile())
        {
            CameraUtils.sendProfileToPlayer(recording.currentProfile(), (EntityPlayerMP) player, false);

            recording.setCurrentProfileTimestamp(System.currentTimeMillis());
        }
    }

    /**
     * When player starts tracking another player, server has to send its
     * morphing values.
     */
    @SubscribeEvent
    public void playerStartsTracking(StartTracking event)
    {
        Entity target = event.getTarget();
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();

        if (target instanceof EntityPlayer)
        {
            IMorphing cap = Morphing.get((EntityPlayer) target);

            Dispatcher.sendTo(new PacketMorphPlayer(target.getEntityId(), cap.getModel(), cap.getSkin()), player);
        }

        if (target instanceof EntityActor)
        {
            EntityActor actor = (EntityActor) target;

            if (actor.isPlaying())
            {
                Utils.sendRequestedRecord(actor.getEntityId(), actor.playback.record.filename, player);
            }
        }
    }
}