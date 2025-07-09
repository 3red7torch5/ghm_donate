package ghmodsgroup.ghmods_donate;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.bukkit.Bukkit.getServer;

public final class Ghmods_donate extends JavaPlugin implements TabCompleter {

    File configfile = new File(getDataFolder(), "config.yml");
    YamlConfiguration config = YamlConfiguration.loadConfiguration(configfile);
    File dbfile = new File(getDataFolder(),"time.yml");
    YamlConfiguration db = YamlConfiguration.loadConfiguration(dbfile);
    File memoryfile = new File(getDataFolder(),"memory.yml");
    YamlConfiguration mem = YamlConfiguration.loadConfiguration(memoryfile);

    private String notificationPrefix;

    @Override
    public void onEnable() {
        getLogger().info("  ____ _   _ __  __           _     \n" +
                "  / ___| | | |  \\/  | ___   __| |___ \n" +
                " | |  _| |_| | |\\/| |/ _ \\ / _` / __|\n" +
                " | |_| |  _  | |  | | (_) | (_| \\__ \\\n" +
                "  \\____|_| |_|_|  |_|\\___/ \\__,_|___/\n" +
                "                                     ");
        config.addDefault("notificationPrefix", "[Время] ");
        config.options().copyDefaults(true);
        this.saveDefaultConfig();
        try {
            db.save(getDataFolder()+"/time.yml");
            mem.save(getDataFolder()+"/memory.yml");
        } catch (Exception e) {
            getLogger().warning(e.getMessage());
        }
        try {
            notificationPrefix = (String) config.get("notificationPrefix");
        } catch (Exception e) {
            getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabled ghmods donation plugin");
        try {
            db.save(getDataFolder()+"/time.yml");
        } catch (Exception e) {
            getLogger().warning(e.getMessage());
        }
    }

    public void onPlayerJoin(PlayerJoinEvent event) {
        updateScores();
    }

    private void createPlayer(String playerName) {
        try {
            db.set(playerName,0);
        } catch (Exception e) {
            getLogger().warning("Failed to create player: " + e.getMessage());
        }
    }

    public int getTime(String playerName) {
        try {
            return (int) db.get(playerName);
        } catch (Exception e) {
            getLogger().warning("Failed to get time: " + e.getMessage());
            return 0;
        }
    }

    public void setTime(String playerName, int amount) {
        try {
            if (!hasAccount(playerName)) {
                createPlayer(playerName);
            }
            db.set(playerName,amount);
        } catch (Exception e) {
            getLogger().warning("Failed to set time: " + e.getMessage());
        }
    }

    public void addTime(String playerName, int amount) {
        setTime(playerName, getTime(playerName) + amount);
        try {
            db.save(getDataFolder()+"/time.yml");
        } catch (Exception e) {
            getLogger().warning(e.getMessage());
        }
    }

    public void removeTime(String playerName, int amount) {
        setTime(playerName, getTime(playerName) - amount);
        try {
            db.save(getDataFolder()+"/time.yml");
        } catch (Exception e) {
            getLogger().warning(e.getMessage());
        }
    }

    public boolean hasAccount(String playerName) {
        try {
            return (db.get(playerName,null) != null);
        } catch (Exception e) {
            getLogger().warning("Failed to check player existence: " + e.getMessage());
            return false;
        }
    }

    public void updateScores() {
        for(Player p : getServer().getOnlinePlayers()) {
            Bukkit.dispatchCommand(getServer().getConsoleSender(),
                    "scoreboard players set " + p.getName() + " time " + getTime(p.getName()));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("время")) {
            if (args.length == 0) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only players can check their balance!");
                    return true;
                }

                Player player = (Player) sender;
                int time = getTime(player.getName());
                player.sendMessage(notificationPrefix + "§9У тебя §6" + time + " §aсекунд");
                return true;
            }
            String action = args[0].toLowerCase();
            if (action.equals("залить") && args.length == 1) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    Inventory inv = player.getInventory();
                    boolean doshit = false;
                    for(ItemStack item : inv.getContents()) {
                        if (item != null){
                            try {
                                if (item.toString().contains("TIAB_TIME_IN_A_BOTTLE")) {
                                    doshit = true;
                                    break;
                                }
                            } catch (Exception e) {
                                getLogger().info("Failed to get item: " + e.getMessage());
                            }
                        }
                    }
                    if (doshit) {
                        Player plr = ((Player) sender);
                        String playerName = plr.getName();
                        int timeToAdd = getTime(playerName);
                        getServer().dispatchCommand(getServer().getConsoleSender(),
                                "execute as " + playerName + " at @s run playsound create_dd:shimmer_fill master @s ~ ~ ~ 1 2 0");
                        if (timeToAdd <= 31104000) { // команда добавляет до 360 дней
                            if (plr.isOp()) {
                                plr.performCommand("tiab addTime " + timeToAdd);
                            } else {
                                try {
                                    plr.setOp(true);
                                    plr.performCommand("tiab addTime " + timeToAdd);
                                    setTime(playerName, 0);
                                } finally {
                                    plr.setOp(false);
                                }
                            }
                            removeTime(playerName,timeToAdd);
                        } else {
                            if (plr.isOp()) {
                                plr.performCommand("tiab addTime 31104000");
                            } else {
                                try {
                                    plr.setOp(true);
                                    plr.performCommand("tiab addTime 31104000");
                                    setTime(playerName, 0);
                                } finally {
                                    plr.setOp(false);
                                }
                            }
                            removeTime(playerName, 31104000);
                        }
                        updateScores();
                        sender.sendMessage(notificationPrefix + "§9В бутылку залито §6" + timeToAdd + " §aсекунд");
                    } else {
                        sender.sendMessage(notificationPrefix + "§9Бутылки не найдено");
                    }
                }
                return true;
            }
            
            if (sender.hasPermission("donation.admin")) {
                if (action.equals("запомнить") && args.length == 1) {
                    List<String> onlinePlayers = List.of();
                    for (Player p : getServer().getOnlinePlayers()) {
                        onlinePlayers.add(p.getName());
                    }
                    try {
                        mem.set("playerList",onlinePlayers);
                        mem.save("memory.yml");
                    } catch (Exception e) {
                        getLogger().warning(e.getMessage());
                    }
                    return true;
                }
                if (action.equals("компенсация")) {
                    if (args.length == 2) {
                        int amount = Integer.valueOf(args[1]);
                        for (Player p : getServer().getOnlinePlayers()) {
                            addTime(p.getName(), amount);
                            p.sendMessage(notificationPrefix + "§9Вам зачислена компенсация §6" + amount + " §aсекунд");
                            getServer().dispatchCommand(getServer().getConsoleSender(),
                                    "execute as " + p.getName() + " at @s run playsound ars_nouveau:ea_finish master @s ~ ~ ~ 1 2 0");
                        }
                    } else if (args.length == 3 && args[-1].equals("вспомнить")) {
                        int amount = Integer.valueOf(args[1]);
                        for (String p : (List<String>) mem.get("playerList")) {
                            addTime(p,amount);
                        }
                    }
                    updateScores();
                    return true;
                }
                if (args.length >= 2) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(notificationPrefix +"§c"+ args[1] + " не в сети, взаимодействую офлайн");
                        return true;
                    }
                    try {
                        int amount = Integer.valueOf(args[2]);
                        String targetName = "Steve";
                        if (target != null) {
                            targetName = target.getName();
                        }
                        if (action.equals("добавить") && args.length == 3) {
                            if (target == null){
                                addTime(args[1],amount);
                            } else {
                                addTime(targetName, amount);
                                target.sendMessage(notificationPrefix + "§9Вам зачислено §6" + amount + " §aсекунд");
                                getServer().dispatchCommand(getServer().getConsoleSender(),
                                        "execute as " + targetName + " at @s run playsound ars_nouveau:ea_finish master @s ~ ~ ~ 1 2 0");
                            }
                            updateScores();
                            return true;
                        } else if (action.equals("убавить") && args.length == 3) {
                            if (target == null) {
                                removeTime(args[1],amount);
                            } else {
                                removeTime(targetName, amount);
                                target.sendMessage(notificationPrefix + "§9У вас забрано §6" + amount + " §aсекунд");
                                getServer().dispatchCommand(getServer().getConsoleSender(),
                                        "execute as " + targetName + " at @s run playsound hexcasting:abacus.shake master @s ~ ~ ~ 1 0.5 0");
                            }
                            updateScores();
                            return true;
                        } else if (action.equals("установить") && args.length == 3) {
                            if (target == null){
                                setTime(args[1],amount);
                            } else {
                                setTime(targetName, amount);
                                target.sendMessage(notificationPrefix + "§9Ваше время теперь §6" + amount + " §aсекунд");
                                getServer().dispatchCommand(getServer().getConsoleSender(),
                                        "execute as " + targetName + " at @s run playsound create:confirm master @s ~ ~ ~ 1 0.1 0");
                            }
                            updateScores();
                            return true;
                        } else if (action.equals("проверить") && args.length == 2) {
                            sender.sendMessage(String.valueOf(getTime(targetName)));
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cСколько блять? Хуйню не пиши");
                        return true;
                    }
                }
            }
            else {
                sender.sendMessage(notificationPrefix + "У тебя нет прав лол");
            }

            // Invalid command usage
            sendUsage(sender);
            return true;
        }
        return false;
    }

    private void sendUsage(CommandSender sender) {
        if (sender.hasPermission("donation.admin")) {
            sender.sendMessage("§cИспользование:");
            sender.sendMessage("§6/время §f- Проверить баланс");
            sender.sendMessage("§6/время добавить <ник> <количество> §f- Добавить время по нику");
            sender.sendMessage("§6/время убавить <ник> <количество> §f- Убавить время по нику");
            sender.sendMessage("§6/время компенсация <количество> §f- Добавить время всем на сервере");
            sender.sendMessage("§6/время запомнить §f- Запоминает всех онлайн игроков");
            sender.sendMessage("§6/время компенсация <количество> вспомнить §f- Добавить время всем из памяти");
            sender.sendMessage("§6/время залить §f- Залить всё время с баланса в бутылку в инвентаре");
        } else {
            sender.sendMessage("§cИспользование:");
            sender.sendMessage("§6/время §f- Проверить баланс");
            sender.sendMessage("§6/время залить §f- Залить всё время с баланса в бутылку в инвентаре");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (cmd.getName().equalsIgnoreCase("время")) {
            if (args.length == 1) {
                if (sender.hasPermission("donation.admin")) {
                    completions.add("добавить");
                    completions.add("убавить");
                    completions.add("компенсация");
                    completions.add("установить");
                }
                completions.add("залить");
            } else if (args.length == 2 &&
                    (args[0].equalsIgnoreCase("добавить") ||
                            args[0].equalsIgnoreCase("убавить") ||
                            args[0].equalsIgnoreCase("установить"))) {
                // Tab complete player names
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            } else if (args.length == 3 && args[1].equals("компенсация")) {
                completions.add("вспомнить");
            }
        }

        return completions;
    }
}