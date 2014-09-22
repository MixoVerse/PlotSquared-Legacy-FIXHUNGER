/*
 * Copyright (c) IntellectualCrafters - 2014.
 * You are not allowed to distribute and/or monetize any of our intellectual property.
 * IntellectualCrafters is not affiliated with Mojang AB. Minecraft is a trademark of Mojang AB.
 *
 * >> File = Main.java
 * >> Generated by: Citymonstret at 2014-08-09 01:43
 */

package com.intellectualcrafters.plot;

import ca.mera.CameraAPI;
import com.intellectualcrafters.plot.Logger.LogLevel;
import com.intellectualcrafters.plot.Settings.Web;
import com.intellectualcrafters.plot.commands.Camera;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.database.DBFunc;
import com.intellectualcrafters.plot.database.MySQL;
import com.intellectualcrafters.plot.database.PlotMeConverter;
import com.intellectualcrafters.plot.events.PlayerTeleportToPlotEvent;
import com.intellectualcrafters.plot.listeners.PlayerEvents;
import com.intellectualcrafters.plot.listeners.WorldEditListener;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import me.confuser.barapi.BarAPI;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

import static org.bukkit.Material.*;

/**
 * @awesome @author Citymonstret, Empire92
 *         PlotMain class.
 */
public class PlotMain extends JavaPlugin {

    /**
     * settings.properties
     */
    public static File configFile;
    public static YamlConfiguration config;
    private static int config_ver = 1;
    /**
     * storage.properties
     */
    public static File storageFile;
    public static YamlConfiguration storage;
    public static int storage_ver = 1;
    /**
     * translations.properties
     */
    public static File translationsFile;
    public static YamlConfiguration translations;
    public static int translations_ver = 1;
    /**
     * MySQL Object
     */
    private static MySQL mySQL;
    /**
     * MySQL Connection
     */
    public static Connection connection;
    /**
     * WorldEdit object
     */
    public static WorldEditPlugin worldEdit = null;
    /**
     * BarAPI object
     */
    public static BarAPI barAPI = null;
    /**
     * CameraAPI object
     */
    public static CameraAPI cameraAPI;

    /**
     * !!WorldGeneration!!
     */
    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldname, String id) {
        return new WorldGenerator(worldname);
    }

    /**
     * TODO: Implement better system
     *
    */
    @SuppressWarnings("deprecation")
    public static void checkForExpiredPlots() {
        final JavaPlugin plugin = PlotMain.getMain();
        Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin,
                new Runnable() {
                    @Override
                    public void run() {
                        checkExpired(plugin, true);
                    }
                }, 0l, 12 * 60 * 60 * 20l);
    }

    /**
     * All loaded plots
     */
    private static HashMap<String, HashMap<PlotId, Plot>> plots;
    /**
     * All loaded plot worlds
     */
    private static HashMap<String, PlotWorld> worlds = new HashMap<String, PlotWorld>();

    /**
     * Get all plots
     *
     * @return HashMap containing the plot ID and the plot object.
     */
    public static Set<Plot> getPlots() {
        ArrayList<Plot> myplots = new ArrayList<Plot>();
        for (HashMap<PlotId, Plot> world:plots.values()) {
            myplots.addAll(world.values());
        }
        return new HashSet<Plot>(myplots);
    }
    
    public static Set<Plot> getPlots(Player player) {
        UUID uuid = player.getUniqueId();
        ArrayList<Plot> myplots = new ArrayList<Plot>();
        for (HashMap<PlotId, Plot> world:plots.values()) {
            for (Plot plot:world.values()) {
                if (plot.getOwner().equals(uuid)) {
                    myplots.add(plot);
                }
            }
        }
        return new HashSet<Plot>(myplots);
    }
    public static Set<Plot> getPlots(World world, Player player) {
        UUID uuid = player.getUniqueId();
        ArrayList<Plot> myplots = new ArrayList<Plot>();
        for (Plot plot:getPlots(world).values()) {
            if (plot.getOwner().equals(uuid)) {
                myplots.add(plot);
            }
        }
        return new HashSet<Plot>(myplots);
    }
    
    public static HashMap<PlotId, Plot> getPlots(World world) {
        if (plots.containsKey(world.getName())) {
            return plots.get(world.getName());
        }
        return new HashMap<PlotId, Plot>();
    }
    /**
     * get all plot worlds
     */
    public static String[] getPlotWorlds() {
        return (worlds.keySet().toArray(new String[0]));
    }
    public static String[] getPlotWorldsString() {
        return plots.keySet().toArray(new String[0]);
    }
    public static boolean isPlotWorld(World world) {
        return (worlds.containsKey(world.getName()));
    }
    public static PlotWorld getWorldSettings(World world) {
        if (worlds.containsKey(world.getName()))
            return worlds.get(world.getName());
        return null;
    }
    public static PlotWorld getWorldSettings(String world) {
        if (worlds.containsKey(world))
            return worlds.get(world);
        return null;
    }
    /**
    *
    * @param world
    * @return set containing the plots for a world
    */
   public static Plot[] getWorldPlots(World world) {
       return (Plot[])(plots.get(world.getName()).values().toArray(new Plot[0]));
   }

    public static void removePlot(String world, PlotId id) {
        plots.get(world).remove(id);
    }
    /**
     * Replace the plot object with an updated version
     *
     * @param id   plot Id
     * @param plot plot object
     */
    public static void updatePlot(Plot plot) {
        String world = plot.world;
        if (!plots.containsKey(world))
            plots.put(world,new HashMap<PlotId, Plot>());
        plot.hasChanged = true;
        plots.get(world).put(plot.id, plot);
    }

    /**
     * TODO: Implement better system
     *
     * @param plugin Plugin
     * @param async  Call async?
     */
    private static void checkExpired(JavaPlugin plugin, boolean async) {
        if (async) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    for (String world:getPlotWorldsString()) {
                        if (plots.containsKey(world)) {
                            for (Plot plot : plots.get(world).values()) {
                                if (plot.owner == null) {
                                    continue;
                                }
                                if (PlayerFunctions.hasExpired(plot)) {
                                    DBFunc.delete(world, plot);
                                }
                            }
                        }
                    }
                }
            });
        } else {
            for (String world:getPlotWorldsString()) {
                if (PlotMain.plots.containsKey(world)) {
                    for (Plot plot : PlotMain.plots.get(world).values()) {
                        if (PlayerFunctions.hasExpired(plot)) {
                            DBFunc.delete(world, plot);
                        }
                    }
                }
            }
        }
    }

    /**
     * On Load.
     * TODO: Load updates async
     */
    @Override
    @SuppressWarnings("deprecation")
    public void onEnable() {
        {
            File log = new File(getMain().getDataFolder() + File.separator + "logs" + File.separator + "plots.log");
            if (!log.exists()) {
                try {
                    if (!new File(getMain().getDataFolder() + File.separator + "logs").mkdirs()) {
                        sendConsoleSenderMessage(C.PREFIX.s() + "&cFailed to create logs folder. Do it manually.");
                    }
                    if (log.createNewFile()) {
                        FileWriter writer = new FileWriter(log);
                        writer.write("Created at: " + new Date().toString() + "\n\n\n");
                        writer.close();
                    }
                } catch (IOException e) {

                    e.printStackTrace();
                }
            }
            Logger.setup(log);
            Logger.add(LogLevel.GENERAL, "Logger enabled");
        }
        configs();

        // TODO make this configurable
        PlotWorld.BLOCKS = new ArrayList<>(Arrays.asList(new Material[]{
                ACACIA_STAIRS, BEACON, BEDROCK,
                BIRCH_WOOD_STAIRS, BOOKSHELF,
                BREWING_STAND, BRICK, BRICK_STAIRS,
                BURNING_FURNACE, CAKE_BLOCK,
                CAULDRON, CLAY_BRICK, CLAY,
                COAL_BLOCK, COAL_ORE, COBBLE_WALL,
                COBBLESTONE, COBBLESTONE_STAIRS,
                COMMAND, DARK_OAK_STAIRS,
                DAYLIGHT_DETECTOR, DIAMOND_ORE,
                DIAMOND_BLOCK, DIRT, DISPENSER,
                DROPPER, EMERALD_BLOCK, EMERALD_ORE,
                ENCHANTMENT_TABLE, ENDER_PORTAL_FRAME,
                ENDER_STONE, FURNACE, GLOWSTONE,
                GOLD_ORE, GOLD_BLOCK, GRASS,
                GRAVEL, GLASS, HARD_CLAY,
                HAY_BLOCK, HUGE_MUSHROOM_1,
                HUGE_MUSHROOM_2, IRON_BLOCK,
                IRON_ORE, JACK_O_LANTERN, JUKEBOX,
                JUNGLE_WOOD_STAIRS, LAPIS_BLOCK,
                LAPIS_ORE, LEAVES, LEAVES_2,
                LOG, LOG_2, MELON_BLOCK,
                MOB_SPAWNER, MOSSY_COBBLESTONE,
                MYCEL, NETHER_BRICK,
                NETHER_BRICK_STAIRS, NETHERRACK,
                NOTE_BLOCK, OBSIDIAN, PACKED_ICE,
                PUMPKIN, QUARTZ_BLOCK, QUARTZ_ORE,
                QUARTZ_STAIRS, REDSTONE_BLOCK,
                SANDSTONE, SAND, SANDSTONE_STAIRS,
                SMOOTH_BRICK, SMOOTH_STAIRS,
                SNOW_BLOCK, SOUL_SAND, SPONGE,
                SPRUCE_WOOD_STAIRS, STONE, WOOD,
                WOOD_STAIRS, WORKBENCH, WOOL,
                getMaterial(44), getMaterial(126)}));
        if (Settings.KILL_ROAD_MOBS)
            killAllEntities();

        if (C.ENABLED.s().length() > 0) {
            Broadcast(C.ENABLED);
        }
        if (Settings.DB.USE_MYSQL) {
            try {
                mySQL = new MySQL(this, Settings.DB.HOST_NAME, Settings.DB.PORT,
                        Settings.DB.DATABASE, Settings.DB.USER,
                        Settings.DB.PASSWORD);
                connection = mySQL.openConnection();
                {
                    DatabaseMetaData meta = connection.getMetaData();
                    ResultSet res = meta.getTables(null, null, "plot", null);
                    if(!res.next())
                        DBFunc.createTables();
                }
            } catch (ClassNotFoundException | SQLException e) {
                Logger.add(LogLevel.DANGER, "MySQL connection failed.");
                System.out.print("\u001B[31m[Plots] MySQL is not setup correctly. The plugin will disable itself.\u001B[0m");
                System.out.print("\u001B[36m==== Here is an ugly stacktrace if you are interested in those things ====\u001B[0m");
                e.printStackTrace();
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
            plots = DBFunc.getPlots();
            
        } else if (Settings.DB.USE_MONGO) {
            sendConsoleSenderMessage(C.PREFIX.s() + "MongoDB is not yet implemented");
        } else {
            Logger.add(LogLevel.DANGER, "No storage type is set.");
            sendConsoleSenderMessage(C.PREFIX + "&cNo storage type is set!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (getServer().getPluginManager().getPlugin("PlotMe") != null ) {
            try {
                new PlotMeConverter(this).runAsync();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        getCommand("plots").setExecutor(new MainCommand());
        getCommand("plots").setAliases(
                new ArrayList<String>() {
                    {
                        add("p");
                        add("plotme");
                        add("plot");
                    }
                });
        getServer().getPluginManager().registerEvents(new PlayerEvents(), this);

        if (getServer().getPluginManager().getPlugin("CameraAPI") != null ){
            cameraAPI = CameraAPI.getInstance();
            Camera camera = new Camera();
            MainCommand.subCommands.add(camera);
            getServer().getPluginManager().registerEvents(camera, this);
        }
        if (getServer().getPluginManager().getPlugin("BarAPI") != null) {
            barAPI = (BarAPI) getServer().getPluginManager().getPlugin("BarAPI");
        }
        if (getServer().getPluginManager().getPlugin("WorldEdit") != null) {
            worldEdit = (WorldEditPlugin) getServer().getPluginManager()
                    .getPlugin("WorldEdit");
            getServer().getPluginManager().registerEvents(
                    new WorldEditListener(), this);
        }
        checkExpired(PlotMain.getMain(), true);
        checkForExpiredPlots();

        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Lag(),
                100L, 1L);

        if(Web.ENABLED) {
            sendConsoleSenderMessage("This is not yet implemented...");
        }
    }

    /**
     * Get MySQL Connection
     *
     * @return connection MySQL Connection.
     */
    @SuppressWarnings("unused")
    public static Connection getConnection() {
        return connection;
    }

    /** .. */

    //Old Stuff
    /*private static boolean checkForUpdate() throws IOException {
		URL call = new URL(Settings.Update.VERSION_URL);
		InputStream stream = call.openStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		String latest = reader.readLine();
		reader.close();
		return !getPlotMain().getDescription().getVersion().equalsIgnoreCase(latest);
	}

	private static String getNextUpdateString() throws IOException {
		URL call = new URL(Settings.Update.VERSION_URL);
		InputStream stream = call.openStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		return reader.readLine();
	}
	
	private static void update() throws IOException {
		sendConsoleSenderMessage(C.PREFIX.s() + "&c&lThere is an update! New Update: &6&l" + getNextUpdateString() + "&c&l, Current Update: &6&l" + getPlotMain().getDescription().getVersion());
	}
	*/

    /**
     * Send a message to the console.
     *
     * @param string message
     */
    public static void sendConsoleSenderMessage(String string) {
        getMain().getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', string));
    }

    public static boolean teleportPlayer(Player player, Location from, Plot plot) {
        PlayerTeleportToPlotEvent event = new PlayerTeleportToPlotEvent(player, from, plot);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if(!event.isCancelled()) {
            Location location = PlotHelper.getPlotHome(Bukkit.getWorld(plot.world), plot);
            if(location.getBlockX() >= 29999999 || location.getBlockX() <= -29999999 ||
                    location.getBlockZ() >= 299999999 ||location.getBlockZ() <= -29999999) {
                event.setCancelled(true);
                return false;
            }
            player.teleport(location);
            PlayerFunctions.sendMessage(player, C.TELEPORTED_TO_PLOT);
        }
        return event.isCancelled();
    }

    /**
     * Send a message to the console
     *
     * @param c message
     */
    @SuppressWarnings("unused")
    public static void sendConsoleSenderMessage(C c) {
        sendConsoleSenderMessage(c.s());
    }

    /**
     * Broadcast publicly
     *
     * @param c message
     */
    public static void Broadcast(C c) {
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                C.PREFIX.s() + c.s()));
    }

    /**
     * Returns the main class.
     *
     * @return (this class)
     */
    public static PlotMain getMain() {
        return JavaPlugin.getPlugin(PlotMain.class);
    }

    /**
     * Broadcast a message to all admins
     *
     * @param c message
     */
    public static void BroadcastWithPerms(C c) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("plots.admin"))
                PlayerFunctions.sendMessage(player, c);
        }
        System.out.println(ChatColor.stripColor(ChatColor
                .translateAlternateColorCodes('&', C.PREFIX.s() + c.s())));
    }


    public static void reloadTranslations() throws IOException {
        translations = YamlConfiguration.loadConfiguration(translationsFile);
    }
    /**
     * Load configuration files
     */
    @SuppressWarnings("deprecation")
    public static void configs() {
        File folder = new File(getMain().getDataFolder() + File.separator + "config");
        if (!folder.exists() && !folder.mkdirs()) {
            sendConsoleSenderMessage(C.PREFIX.s() + "&cFailed to create the /plugins/config folder. Please create it manually.");
        }
        try {
            configFile = new File(getMain().getDataFolder() + File.separator + "config" + File.separator +"settings.yml");
            if (!configFile.exists()) {
                configFile.createNewFile();
            }
            config = YamlConfiguration.loadConfiguration(configFile);
            setupConfig();
        }
        catch (Exception err_trans) {
            Logger.add(LogLevel.DANGER, "Failed to save settings.yml");
            System.out.println("Failed to save settings.yml");
        }
        try {
            storageFile = new File(getMain().getDataFolder() + File.separator + "config" + File.separator +"storage.yml");
            if (!storageFile.exists()) {
                storageFile.createNewFile();
            }
            storage = YamlConfiguration.loadConfiguration(storageFile);
            setupStorage();
        }
        catch (Exception err_trans) {
            Logger.add(LogLevel.DANGER, "Failed to save storage.yml");
            System.out.println("Failed to save storage.yml");
        }
        try {
            translationsFile = new File(getMain().getDataFolder() + File.separator + "config" + File.separator +"translations.yml");
            if (!translationsFile.exists()) {
                translationsFile.createNewFile();
            }
            translations = YamlConfiguration.loadConfiguration(translationsFile);
            setupTranslations();
        }
        catch (Exception err_trans) {
            Logger.add(LogLevel.DANGER, "Failed to save translations.yml");
            System.out.println("Failed to save translations.yml");
        }
        
        
        try {
            config.save(configFile);
            storage.save(storageFile);
            translations.save(translationsFile);
        } catch (IOException e) {
            Logger.add(LogLevel.DANGER, "Configuration file saving failed");
            e.printStackTrace();
        }
        {
            Settings.DB.USE_MYSQL = true;
            Settings.DB.USER = storage.getString("mysql_user");
            Settings.DB.PASSWORD = storage.getString("mysql_password");
            Settings.DB.HOST_NAME = storage.getString("mysql_host");
            Settings.DB.PORT = storage.getString("mysql_port");
            Settings.DB.DATABASE = storage.getString("mysql_database");
        }
        {
            Settings.Update.AUTO_UPDATE = config.getBoolean("auto_update");
            
            
            
            //Web
            Web.ENABLED = config.getBoolean("web.enabled");
            Web.PORT    = config.getInt("web.port");
        }
    }

    /**
     * Kill all entities on roads
     */
    @SuppressWarnings("deprecation")
    public static void killAllEntities() {
        Bukkit.getScheduler().scheduleAsyncRepeatingTask(getMain(),
                new Runnable() {
                    World world;
                    Location location;
                    long ticked = 0l;
                    long error = 0l;
                    @Override
                    public void run() {
                        for (String w: getPlotWorlds()) {
                            World world = Bukkit.getWorld(w);
                             try {
                                if(world.getLoadedChunks().length < 1) {
                                    return;
                                }
                                for (Chunk chunk : world.getLoadedChunks()) {
                                    for (Entity entity : chunk.getEntities()){
                                        if (entity.getType() == EntityType.PLAYER)
                                            continue;
                                        location = entity.getLocation();
                                        if (!PlayerEvents.isInPlot(location))
                                            entity.remove();
                                    }
                                }
                            } catch (Exception e) {
                                ++error;
                            }
                            finally {
                                 ++ticked;
                            }
                        }
                    }
                }, 0l, 2l);
    }

    /**
     * SETUP: settings.properties
     */
    private static void setupConfig() {
        config.set("version", config_ver);
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("auto_update", false);
        options.put("kill_road_mobs", Settings.KILL_ROAD_MOBS_DEFAULT);
        options.put("web.enabled", Web.ENABLED);
        options.put("web.port", Web.PORT);
        for (Entry<String, Object> node : options.entrySet()) {
            if (!config.contains(node.getKey())) {
                config.set(node.getKey(), node.getValue());
            }
        }
        for (String node:config.getConfigurationSection("worlds").getKeys(false)) {
            World world = Bukkit.getWorld(node);
            if (world==null) {
                Logger.add(LogLevel.WARNING, "World '"+node+"' in settings.yml does not exist (case sensitive)");
            }
            else {
                ChunkGenerator gen = world.getGenerator();
                if (gen==null || gen.toString().equals("PlotSquared")) {
                    Logger.add(LogLevel.WARNING, "World '"+node+"' in settings.yml is not using PlotSquared generator");
                }
            }
        }
    }

    /**
     * SETUP: storage.properties
     */
    private static void setupStorage() {
        storage.set("version", storage_ver);
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("mysql_host", "localhost");
        options.put("mysql_port", "3306");
        options.put("mysql_user", "root");
        options.put("mysql_password", "password");
        options.put("mysql_database", "plot_db");
        for (Entry<String, Object> node : options.entrySet()) {
            if (!storage.contains(node.getKey())) {
                storage.set(node.getKey(), node.getValue());
            }
        }
    }

    /**
     * SETUP: translations.properties
     */
    public static void setupTranslations() {
        translations.set("version", translations_ver);
        for (C c : C.values()) {
            if (!translations.contains(c.toString())) {
                translations.set(c.toString(), c.s());
            }
           
        }
    }

    /**
     * On unload
     */
    @Override
    public void onDisable() {
        Logger.add(LogLevel.GENERAL, "Logger disabled");
        try {
            Logger.write();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {
            connection.close();
            mySQL.closeConnection();
        }
        catch (NullPointerException | SQLException e) {
            if (connection!=null) {
                Logger.add(LogLevel.DANGER, "Could not close mysql connection");
            }
        }
        /*
        if(PlotWeb.PLOTWEB != null) {
            try {
                PlotWeb.PLOTWEB.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/
    }
    public static void addPlotWorld(String world,PlotWorld plotworld) {
        PlotMain.worlds.put(world,plotworld);
        if(!plots.containsKey(world))
            plots.put(world, new HashMap<PlotId, Plot>());
    }
}