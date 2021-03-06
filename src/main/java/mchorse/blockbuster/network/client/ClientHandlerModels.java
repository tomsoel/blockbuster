package mchorse.blockbuster.network.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import mchorse.blockbuster.Blockbuster;
import mchorse.blockbuster.common.ClientProxy;
import mchorse.blockbuster.network.common.PacketModels;
import mchorse.blockbuster.utils.L10n;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * This handler is responsible for saving received models and skins to
 * directory where downloaded models and skins should be located.
 */
public class ClientHandlerModels extends ClientMessageHandler<PacketModels>
{
    @Override
    @SideOnly(Side.CLIENT)
    public void run(EntityPlayerSP player, PacketModels message)
    {
        String path = ClientProxy.config.getAbsolutePath() + "/downloads";

        try
        {
            int modelSize = 0;
            int skinSize = 0;

            /* Write skins to downloaded folder */
            for (Map.Entry<String, Map<String, ByteBuf>> entry : message.skins.entrySet())
            {
                String modelName = entry.getKey();

                for (Map.Entry<String, ByteBuf> skin : entry.getValue().entrySet())
                {
                    String skinPath = String.format("%s/%s/skins/%s.png", path, modelName, skin.getKey());

                    new File(path + "/" + modelName + "/skins").mkdirs();
                    this.bufferToFile(skinPath, skin.getValue());

                    skinSize++;
                }
            }

            /* Write models to downloaded folder */
            for (Map.Entry<String, String> model : message.models.entrySet())
            {
                String modelName = model.getKey();
                String modelPath = String.format("%s/%s/model.json", path, modelName);

                new File(path + "/" + modelName).mkdirs();
                this.stringToFile(modelPath, model.getValue());

                modelSize++;
            }

            ClientProxy.actorPack.pack.reload();
            Blockbuster.proxy.loadModels(ClientProxy.actorPack.pack);

            L10n.info(player, "models.loaded", modelSize, skinSize);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Write given string to file
     */
    private void stringToFile(String file, String output) throws IOException
    {
        PrintWriter writer = new PrintWriter(file);

        writer.print(output);
        writer.close();
    }

    /**
     * Write given byte buffer
     */
    private void bufferToFile(String string, ByteBuf value) throws IOException
    {
        FileOutputStream output = new FileOutputStream(string);

        byte[] bytes = new byte[value.readableBytes()];

        value.readBytes(bytes);
        output.write(bytes);
        output.close();
    }
}