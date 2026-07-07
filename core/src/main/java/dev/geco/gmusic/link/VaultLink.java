package dev.geco.gmusic.link;

import dev.geco.gmusic.GMusicMain;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class VaultLink {

    private final GMusicMain gMusicMain;
    private Economy economy;

    public VaultLink(GMusicMain gMusicMain) {
        this.gMusicMain = gMusicMain;

        RegisteredServiceProvider<Economy> provider = gMusicMain.getServer().getServicesManager().getRegistration(Economy.class);
        if(provider != null) economy = provider.getProvider();
    }

    public boolean hasEconomy() { return economy != null; }

    public double getBalance(@NotNull OfflinePlayer player) {
        if(economy == null) return 0;
        return economy.getBalance(player);
    }

    public boolean has(@NotNull OfflinePlayer player, double amount) {
        if(economy == null) return false;
        return economy.has(player, amount);
    }

    public boolean withdraw(@NotNull OfflinePlayer player, double amount) {
        if(economy == null) return false;
        try {
            EconomyResponse response = economy.withdrawPlayer(player, amount);
            return response.transactionSuccess();
        } catch(Throwable e) { gMusicMain.getLogger().log(Level.SEVERE, "Could not withdraw from player balance!", e); }
        return false;
    }

    public String format(double amount) {
        if(economy == null) return "" + amount;
        return economy.format(amount);
    }

}
