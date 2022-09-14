package net.bonn2.showtimeout;

import net.bonn2.Bot;
import net.bonn2.modules.Module;
import net.bonn2.modules.settings.Settings;
import net.bonn2.modules.settings.types.Setting;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class Main extends Module {

    public Main() {
        version = "2.0";
        name = "ShowTimeout";
    }

    @Override
    public void registerLoggingChannels() {}

    public static Map<Member, ScheduledExecutorService> SCHEDULED = new HashMap<>();

    @Override
    public void registerSettings() {
        Settings.register(this, "timeout_role", Setting.Type.ROLE, Setting.Type.ROLE.unset,
                "The role to be given to timed out users.");
    }

    @Override
    public void load() {
        Bot.logger.info("Registering Listeners...");
        Bot.jda.addEventListener(new Listener(this));
        Bot.logger.info("Checking Users for old Timeout Roles");
        checkUsers();
    }

    @Override
    public CommandData[] getCommands() {
        return new CommandData[0];
    }

    public void checkUsers() {
        for (Guild guild : Bot.jda.getGuilds()) {
            guild.loadMembers().onSuccess((members -> {
                Role timeoutRole = getTimeoutRole(guild);
                for (Member member : members) {
                    if (member.isTimedOut()) {
                        // Give role
                        if (!member.getRoles().contains(timeoutRole))
                            guild.addRoleToMember(member, timeoutRole).queue();
                        // Schedule role removal
                        scheduleRoleRemoval(member);
                    } else {
                        // Remove role
                        if (member.getRoles().contains(timeoutRole))
                            guild.removeRoleFromMember(member, timeoutRole).queue();
                    }
                }
            }));
        }
    }

    public void scheduleRoleRemoval(@NotNull Member member) {
        Role timeoutRole = getTimeoutRole(member.getGuild());
        // Store executor so it can be canceled
        SCHEDULED.put(member, new ScheduledThreadPoolExecutor(1));
        // Schedule removal after storage to prevent rescheduling
        SCHEDULED.get(member).schedule(
                () -> {
                    member.getGuild().removeRoleFromMember(member, timeoutRole).queue();
                    SCHEDULED.remove(member);
                },
                Objects.requireNonNull(member.getTimeOutEnd()).toEpochSecond() - System.currentTimeMillis() / 1000,
                TimeUnit.SECONDS
        );
    }

    public Role getTimeoutRole(@NotNull Guild guild) {
        return Objects.requireNonNull(Settings.get(this, guild.getId(), "timeout_role")).getAsRole(guild);
    }
}
