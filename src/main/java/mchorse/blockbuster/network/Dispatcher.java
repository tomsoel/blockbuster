package mchorse.blockbuster.network;

import mchorse.blockbuster.Blockbuster;
import mchorse.blockbuster.network.client.ClientHandlerModels;
import mchorse.blockbuster.network.client.ClientHandlerModifyActor;
import mchorse.blockbuster.network.client.ClientHandlerMorph;
import mchorse.blockbuster.network.client.ClientHandlerMorphPlayer;
import mchorse.blockbuster.network.client.camera.ClientHandlerCameraProfile;
import mchorse.blockbuster.network.client.camera.ClientHandlerCameraState;
import mchorse.blockbuster.network.client.camera.ClientHandlerListCameraProfiles;
import mchorse.blockbuster.network.client.director.ClientHandlerDirectorCast;
import mchorse.blockbuster.network.client.recording.ClientHandlerFrames;
import mchorse.blockbuster.network.client.recording.ClientHandlerPlayback;
import mchorse.blockbuster.network.client.recording.ClientHandlerPlayerRecording;
import mchorse.blockbuster.network.client.recording.ClientHandlerRequestedFrames;
import mchorse.blockbuster.network.client.recording.ClientHandlerSyncTick;
import mchorse.blockbuster.network.client.recording.ClientHandlerUnloadFrames;
import mchorse.blockbuster.network.client.recording.ClientHandlerUnloadRecordings;
import mchorse.blockbuster.network.common.PacketCameraMarker;
import mchorse.blockbuster.network.common.PacketModels;
import mchorse.blockbuster.network.common.PacketModifyActor;
import mchorse.blockbuster.network.common.PacketMorph;
import mchorse.blockbuster.network.common.PacketMorphPlayer;
import mchorse.blockbuster.network.common.PacketPlaybackButton;
import mchorse.blockbuster.network.common.PacketRequestModels;
import mchorse.blockbuster.network.common.camera.PacketCameraProfile;
import mchorse.blockbuster.network.common.camera.PacketCameraReset;
import mchorse.blockbuster.network.common.camera.PacketCameraState;
import mchorse.blockbuster.network.common.camera.PacketListCameraProfiles;
import mchorse.blockbuster.network.common.camera.PacketLoadCameraProfile;
import mchorse.blockbuster.network.common.camera.PacketRequestCameraProfiles;
import mchorse.blockbuster.network.common.director.PacketDirectorAdd;
import mchorse.blockbuster.network.common.director.PacketDirectorCast;
import mchorse.blockbuster.network.common.director.PacketDirectorEdit;
import mchorse.blockbuster.network.common.director.PacketDirectorRemove;
import mchorse.blockbuster.network.common.director.PacketDirectorRequestCast;
import mchorse.blockbuster.network.common.director.PacketDirectorReset;
import mchorse.blockbuster.network.common.recording.PacketFramesChunk;
import mchorse.blockbuster.network.common.recording.PacketFramesLoad;
import mchorse.blockbuster.network.common.recording.PacketPlayback;
import mchorse.blockbuster.network.common.recording.PacketPlayerRecording;
import mchorse.blockbuster.network.common.recording.PacketRequestFrames;
import mchorse.blockbuster.network.common.recording.PacketRequestedFrames;
import mchorse.blockbuster.network.common.recording.PacketSyncTick;
import mchorse.blockbuster.network.common.recording.PacketUnloadFrames;
import mchorse.blockbuster.network.common.recording.PacketUnloadRecordings;
import mchorse.blockbuster.network.server.ServerHandlerCameraMarker;
import mchorse.blockbuster.network.server.ServerHandlerModifyActor;
import mchorse.blockbuster.network.server.ServerHandlerMorph;
import mchorse.blockbuster.network.server.ServerHandlerPlaybackButton;
import mchorse.blockbuster.network.server.ServerHandlerRequestModels;
import mchorse.blockbuster.network.server.camera.ServerHandlerCameraProfile;
import mchorse.blockbuster.network.server.camera.ServerHandlerCameraReset;
import mchorse.blockbuster.network.server.camera.ServerHandlerListCameraProfiles;
import mchorse.blockbuster.network.server.camera.ServerHandlerLoadCameraProfile;
import mchorse.blockbuster.network.server.director.ServerHandlerDirectorAdd;
import mchorse.blockbuster.network.server.director.ServerHandlerDirectorEdit;
import mchorse.blockbuster.network.server.director.ServerHandlerDirectorRemove;
import mchorse.blockbuster.network.server.director.ServerHandlerDirectorRequestCast;
import mchorse.blockbuster.network.server.director.ServerHandlerDirectorReset;
import mchorse.blockbuster.network.server.recording.ServerHandlerFramesChunk;
import mchorse.blockbuster.network.server.recording.ServerHandlerPlayback;
import mchorse.blockbuster.network.server.recording.ServerHandlerRequestFrames;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Network dispatcher
 *
 * @author Ernio (Ernest Sadowski)
 */
public class Dispatcher
{
    private static final SimpleNetworkWrapper DISPATCHER = NetworkRegistry.INSTANCE.newSimpleChannel(Blockbuster.MODID);
    private static byte PACKET_ID;

    public static SimpleNetworkWrapper get()
    {
        return DISPATCHER;
    }

    /**
     * Send message to players who are tracking given entity
     */
    public static void sendToTracked(Entity entity, IMessage message)
    {
        EntityTracker tracker = ((WorldServer) entity.worldObj).getEntityTracker();

        for (EntityPlayer player : tracker.getTrackingPlayers(entity))
        {
            DISPATCHER.sendTo(message, (EntityPlayerMP) player);
        }
    }

    /**
     * Send message to given player
     */
    public static void sendTo(IMessage message, EntityPlayerMP player)
    {
        DISPATCHER.sendTo(message, player);
    }

    /**
     * Send message to the server
     */
    public static void sendToServer(IMessage message)
    {
        DISPATCHER.sendToServer(message);
    }

    /**
     * Register all the networking messages and message handlers
     */
    public static void register()
    {
        /* Update actor properties */
        register(PacketModifyActor.class, ClientHandlerModifyActor.class, Side.CLIENT);
        register(PacketModifyActor.class, ServerHandlerModifyActor.class, Side.SERVER);

        /* Recording */
        register(PacketPlayerRecording.class, ClientHandlerPlayerRecording.class, Side.CLIENT);

        register(PacketFramesLoad.class, ClientHandlerFrames.class, Side.CLIENT);
        register(PacketFramesChunk.class, ServerHandlerFramesChunk.class, Side.SERVER);

        register(PacketPlayback.class, ClientHandlerPlayback.class, Side.CLIENT);
        register(PacketPlayback.class, ServerHandlerPlayback.class, Side.SERVER);

        register(PacketRequestFrames.class, ServerHandlerRequestFrames.class, Side.SERVER);
        register(PacketRequestedFrames.class, ClientHandlerRequestedFrames.class, Side.CLIENT);

        register(PacketUnloadFrames.class, ClientHandlerUnloadFrames.class, Side.CLIENT);
        register(PacketUnloadRecordings.class, ClientHandlerUnloadRecordings.class, Side.CLIENT);

        register(PacketSyncTick.class, ClientHandlerSyncTick.class, Side.CLIENT);

        /* Director block management messages */
        register(PacketDirectorCast.class, ClientHandlerDirectorCast.class, Side.CLIENT);

        register(PacketDirectorRequestCast.class, ServerHandlerDirectorRequestCast.class, Side.SERVER);
        register(PacketDirectorReset.class, ServerHandlerDirectorReset.class, Side.SERVER);
        register(PacketDirectorAdd.class, ServerHandlerDirectorAdd.class, Side.SERVER);
        register(PacketDirectorEdit.class, ServerHandlerDirectorEdit.class, Side.SERVER);
        register(PacketDirectorRemove.class, ServerHandlerDirectorRemove.class, Side.SERVER);

        /* Camera management */
        register(PacketCameraProfile.class, ClientHandlerCameraProfile.class, Side.CLIENT);
        register(PacketCameraProfile.class, ServerHandlerCameraProfile.class, Side.SERVER);
        register(PacketCameraReset.class, ServerHandlerCameraReset.class, Side.SERVER);
        register(PacketCameraState.class, ClientHandlerCameraState.class, Side.CLIENT);
        register(PacketLoadCameraProfile.class, ServerHandlerLoadCameraProfile.class, Side.SERVER);

        register(PacketRequestCameraProfiles.class, ServerHandlerListCameraProfiles.class, Side.SERVER);
        register(PacketListCameraProfiles.class, ClientHandlerListCameraProfiles.class, Side.CLIENT);

        register(PacketPlaybackButton.class, ServerHandlerPlaybackButton.class, Side.SERVER);

        register(PacketCameraMarker.class, ServerHandlerCameraMarker.class, Side.SERVER);

        /* Morphing */
        register(PacketMorph.class, ClientHandlerMorph.class, Side.CLIENT);
        register(PacketMorph.class, ServerHandlerMorph.class, Side.SERVER);
        register(PacketMorphPlayer.class, ClientHandlerMorphPlayer.class, Side.CLIENT);

        /* Multiplayer */
        register(PacketModels.class, ClientHandlerModels.class, Side.CLIENT);
        register(PacketRequestModels.class, ServerHandlerRequestModels.class, Side.SERVER);
    }

    /**
     * Register given message with given message handler on a given side
     */
    private static <REQ extends IMessage, REPLY extends IMessage> void register(Class<REQ> message, Class<? extends IMessageHandler<REQ, REPLY>> handler, Side side)
    {
        DISPATCHER.registerMessage(handler, message, PACKET_ID++, side);
    }
}