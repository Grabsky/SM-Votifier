package pl.ibcgames.smvotifier;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.json.simple.JSONObject;

import java.util.*;

public class Reward implements CommandExecutor {
    String token = Votifier.plugin.getConfiguration().get().getString("identyfikator");
    boolean require_permission = Votifier.plugin.getConfiguration().get().getBoolean("wymagaj_uprawnien");
    List<String> list = Votifier.plugin.getConfiguration().get().getStringList("komendy");
    Map<String, Date> timeouts = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Bukkit.getScheduler().runTaskAsynchronously(Votifier.plugin, () -> {
            if (token == null || token.equalsIgnoreCase("tutaj_wpisz_identyfikator")) {
                sender.sendMessage(Utils.message("§cBrak identyfikatora serwera w konfiguracji SM-Votifier"));
                sender.sendMessage(Utils.message("§cWiecej informacji znajdziesz pod adresem:"));
                sender.sendMessage(Utils.message("§ahttps://serwery-minecraft.pl/konfiguracja-pluginu"));
                return;
            }

            if (require_permission && !sender.hasPermission("smvotifier.nagroda")) {
                sender.sendMessage(Utils.message("§8› §cBrak uprawnień."));
                return;
            }

            if (timeouts.containsKey(sender.getName())) {
                Date d = timeouts.get(sender.getName());
                long diff = (long) Math.floor((new Date().getTime() / 1000) - (d.getTime() / 1000));

                if (diff < 60) {
                    long remaining = 60 - diff;
                    sender.sendMessage(Utils.message("§8› §cSpróbuj ponownie za kilka sekund..."));

                    return;
                }
            }
            JSONObject res = Utils.sendRequest("https://serwery-minecraft.pl/api/server-by-key/" + token + "/get-vote/" + sender.getName());
            timeouts.put(sender.getName(), new Date());
            execute(res, sender);
        });
        return true;
    }

    private void execute(JSONObject res, CommandSender sender) {
        boolean canClaimReward = false;

        if (res.containsKey("can_claim_reward")) {
            canClaimReward = Boolean.parseBoolean(
                    res.get("can_claim_reward").toString()
            );
        }

        if (res.containsKey("error")) {
            if (res.get("error").toString().equals("&cNie znaleziono glosu")) {
                sender.sendMessage("§8› §cNie masz żadnej nagrody do odebrania.");
            } else {
                System.out.println("[DEBUG] Error when retrieving reward info for " + sender.getName() + ": " + res);
                sender.sendMessage("§8› §cNie udało się odebrać nagrody. Spróbuj ponownie później.");
            }
            return;
        }

        if (!canClaimReward && !res.containsKey("error")) {
            sender.sendMessage(Utils.message("§8› §cNie masz żadnej nagrody do odebrania."));
            return;
        }

        if (canClaimReward) {
            for (String cmd : list) {
                cmd = cmd.replace("{GRACZ}", sender.getName());
                String finalCmd = cmd;

                Bukkit.getScheduler().scheduleSyncDelayedTask(Votifier.plugin, () -> {
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                }, 0);
            }
        }
    }
}
