package net.bonn2.showtimeout;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class Listener extends ListenerAdapter {

    private final Main main;

    public Listener(Main module) {
        this.main = module;
    }

    @Override
    public void onGuildMemberUpdate(@NotNull GuildMemberUpdateEvent event) {
        Role timeoutRole = main.getTimeoutRole(event.getGuild());
        // Only apply role if role exists
        if (timeoutRole == null) return;
        // Check if user was timed out
        if (event.getMember().isTimedOut()) {
            if (!event.getMember().getRoles().contains(timeoutRole)) {
                // Remove unused schedulers
                if (Main.SCHEDULED.containsKey(event.getMember())) {
                    Main.SCHEDULED.get(event.getMember()).shutdownNow();
                    Main.SCHEDULED.remove(event.getMember());
                }
                // Give role
                event.getGuild().addRoleToMember(event.getMember(), timeoutRole).queue();
                // Schedule role removal
                main.scheduleRoleRemoval(event.getMember());
            }
        } else {
            // Remove old schedulers
            if (Main.SCHEDULED.containsKey(event.getMember())) {
                Main.SCHEDULED.get(event.getMember()).shutdownNow();
                Main.SCHEDULED.remove(event.getMember());
            }
            // Remove role
            if (event.getMember().getRoles().contains(timeoutRole))
                event.getGuild().removeRoleFromMember(event.getMember(), timeoutRole).queue();
        }
    }
}
