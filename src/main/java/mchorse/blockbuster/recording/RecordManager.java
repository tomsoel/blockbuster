package mchorse.blockbuster.recording;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mchorse.blockbuster.capabilities.morphing.Morphing;
import mchorse.blockbuster.common.entity.EntityActor;
import mchorse.blockbuster.network.Dispatcher;
import mchorse.blockbuster.network.common.recording.PacketPlayback;
import mchorse.blockbuster.network.common.recording.PacketPlayerRecording;
import mchorse.blockbuster.recording.actions.Action;
import mchorse.blockbuster.recording.data.FrameChunk;
import mchorse.blockbuster.recording.data.Mode;
import mchorse.blockbuster.recording.data.Record;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Record manager
 *
 * This class responsible is responsible for managing record recorders and
 * players for entity players and actors.
 */
public class RecordManager
{
    /**
     * Loaded records
     */
    public Map<String, Record> records = new HashMap<String, Record>();

    /**
     * Incomplete chunk frame only records (for recording big records)
     */
    public Map<String, FrameChunk> chunks = new HashMap<String, FrameChunk>();

    /**
     * Currently running record recorders (I have something to do about the
     * name)
     */
    public Map<EntityPlayer, RecordRecorder> recorders = new HashMap<EntityPlayer, RecordRecorder>();

    /**
     * No, not {@link EntityPlayer}s, say record pla-yers, pla-yers...
     */
    public Map<EntityActor, RecordPlayer> players = new HashMap<EntityActor, RecordPlayer>();

    /**
     * Get action list for given player
     */
    public List<Action> getActions(EntityPlayer player)
    {
        RecordRecorder recorder = this.recorders.get(player);

        return recorder == null ? null : recorder.actions;
    }

    /**
     * Start recording given player to record with given filename
     */
    public boolean startRecording(String filename, EntityPlayer player, Mode mode, boolean notify)
    {
        if (filename.isEmpty() || this.stopRecording(player, notify))
        {
            return false;
        }

        for (RecordRecorder recorder : this.recorders.values())
        {
            if (recorder.record.filename.equals(filename))
            {
                Utils.broadcastInfo("recording.recording", filename);

                return false;
            }
        }

        this.recorders.put(player, new RecordRecorder(new Record(filename), mode));

        if (notify)
        {
            Dispatcher.sendTo(new PacketPlayerRecording(true, filename), (EntityPlayerMP) player);
        }

        return true;
    }

    /**
     * Stop recording for given player
     */
    public boolean stopRecording(EntityPlayer player, boolean notify)
    {
        RecordRecorder recorder = this.recorders.get(player);

        if (recorder != null)
        {
            String filename = recorder.record.filename;

            this.records.put(filename, recorder.record);
            this.recorders.remove(player);

            if (notify)
            {
                Morphing.get(player).reset();
                Dispatcher.sendTo(new PacketPlayerRecording(false, ""), (EntityPlayerMP) player);
            }

            return true;
        }

        return false;
    }

    /**
     * Start playback from given filename and given actor. You also have to
     * specify the mode of playback.
     */
    public boolean startPlayback(String filename, EntityActor actor, Mode mode, boolean kill, boolean notify)
    {
        if (this.players.containsKey(actor))
        {
            return false;
        }

        try
        {
            File file = Utils.replayFile(filename);

            Record record = new Record(filename);
            record.load(file);
            RecordPlayer player = new RecordPlayer(record, mode);

            actor.playback = player;
            actor.playback.record.applyFrame(0, actor, true);
            actor.playback.kill = kill;

            if (notify)
            {
                Dispatcher.sendToTracked(actor, new PacketPlayback(actor.getEntityId(), true, filename));
            }

            this.records.put(filename, record);
            this.players.put(actor, player);

            return true;
        }
        catch (FileNotFoundException e)
        {
            Utils.broadcastError("recording.not_found", filename);
        }
        catch (Exception e)
        {
            Utils.broadcastError("recording.read", filename);
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Stop playback for the given actor. If the actor doesn't exist in players,
     * it simply does nothing.
     */
    public void stopPlayback(EntityActor actor)
    {
        if (!this.players.containsKey(actor))
        {
            return;
        }

        actor.playback.record.reset(actor);

        if (actor.playback.kill)
        {
            actor.setDead();
        }
        else
        {
            Dispatcher.sendToTracked(actor, new PacketPlayback(actor.getEntityId(), false, ""));
        }

        actor.playback = null;
        this.players.remove(actor);
    }

    /**
     * Reset the tracking manager data
     */
    public void reset()
    {
        this.records.clear();
        this.chunks.clear();
        this.recorders.clear();
        this.players.clear();
    }

    /**
     * Abort the recording of action for given player
     */
    public void abortRecording(EntityPlayer player)
    {
        if (this.recorders.containsKey(player))
        {
            RecordRecorder recorder = this.recorders.remove(player);

            Utils.broadcastError("recording.logout", recorder.record.filename);
        }
    }
}