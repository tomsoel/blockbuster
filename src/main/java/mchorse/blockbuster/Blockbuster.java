package mchorse.blockbuster;

import mchorse.blockbuster.commands.CommandAction;
import mchorse.blockbuster.commands.CommandDirector;
import mchorse.blockbuster.common.CommonProxy;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;

/**
 * <p>Blockbuster's main entry</p>
 *
 * <p>
 * This mod allows you to create machinimas in minecraft. Blockbuster provides
 * you with the most needed tools to create machinimas alone (without bunch of
 * complaining body actors).
 * </p>
 *
 * <p>
 * This mod is possible thanks to the following code/examples/resources/people:
 * </p>
 *
 * <ul>
 *     <li>Jabelar's and TGG's minecraft modding tutorials</li>
 *     <li>AnimalBikes and Mocap mods (EchebKeso)</li>
 *     <li>MinecraftByExample</li>
 *     <li>Ernio for helping with camera attributes sync, sharing with his own
 *         network abstract layer code, and fixing the code so it would work on
 *         dedicated server</li>
 *     <li>diesieben07 for giving idea for actor skins</li>
 *     <li>Choonster for pointing out that processInteract triggers for each
 *         hand + TestMod3 config example</li>
 *     <li>Lightwave for porting some of the code to 1.9.4</li>
 *     <li>NlL5 for a lot of testing, giving lots of feedback and ideas for
 *         Blockbuster mod</li>
 *     <li>daipenger for giving me consultation on how to make cameras and
 *         actors frame-based</li>
 * </ul>
 */
@Mod(modid = Blockbuster.MODID, name = Blockbuster.MODNAME, version = Blockbuster.VERSION, guiFactory = Blockbuster.GUI_FACTORY)
public class Blockbuster
{
    /* Mod info */
    public static final String MODID = "blockbuster";
    public static final String MODNAME = "Blockbuster";
    public static final String VERSION = "1.4.5";
    public static final String GUI_FACTORY = "mchorse.blockbuster.config.gui.GuiFactory";

    /* Proxies */
    public static final String CLIENT_PROXY = "mchorse.blockbuster.common.ClientProxy";
    public static final String SERVER_PROXY = "mchorse.blockbuster.common.CommonProxy";

    /* Creative tab */
    public static CreativeTabs blockbusterTab;

    /* Items */
    public static Item playbackItem;
    public static Item registerItem;
    public static Item actorConfigItem;

    /* Blocks */
    public static Block directorBlock;

    /* Forge stuff */
    @Mod.Instance
    public static Blockbuster instance;

    @SidedProxy(clientSide = CLIENT_PROXY, serverSide = SERVER_PROXY)
    public static CommonProxy proxy;

    /**
     * "Macro" for getting resource location for Blockbuster mod items,
     * entities, blocks, etc.
     */
    public static String path(String path)
    {
        return MODID + ":" + path;
    }

    @EventHandler
    public void preLoad(FMLPreInitializationEvent event)
    {
        proxy.preLoad(event);
    }

    @EventHandler
    public void load(FMLInitializationEvent event)
    {
        proxy.load(event);
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        String path = DimensionManager.getCurrentSaveRootDirectory() + "/blockbuster/models";

        proxy.models.pack = proxy.getPack();
        proxy.models.pack.addFolder(path);
        proxy.loadModels(proxy.models.pack);

        event.registerServerCommand(new CommandAction());
        event.registerServerCommand(new CommandDirector());
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event)
    {
        CommonProxy.manager.reset();
    }
}