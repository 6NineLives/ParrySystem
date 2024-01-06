package simpleparrying.simpleparry;

import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleParry extends JavaPlugin {

    public static SimpleParry plugin;

    @Override
    public void onEnable() {
        // Plugin startup logic
        System.out.println("My first plugin has started!!! Hello!!!");
        plugin = this;
        new Parry(this);
        loadConfig();
    }
    private void loadConfig() {
        // Save default config.yml if it doesn't exist
        this.saveDefaultConfig();

        // Load configuration settings
        double initialParryWindow = this.getConfig().getDouble("initialParryWindow");
        double penaltyMultiplier = this.getConfig().getDouble("penaltyMultiplier");
        double penaltyResetTime = this.getConfig().getDouble("penaltyResetTime");
        double tapDuration = this.getConfig().getDouble("tapDuration");

        // You can now use these variables in your code
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        System.out.println("My first plugin has stopped!!! Bye!!!");
    }
}


