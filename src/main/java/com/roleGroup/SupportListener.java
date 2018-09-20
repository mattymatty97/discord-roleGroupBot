package com.roleGroup;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("Duplicates")
public class SupportListener extends ListenerAdapter {
    static final long supportID = 428163245753499653L;
    
    private Role botRole;

    private long roleID;

    @Override
    public void onReady(ReadyEvent event) {
        Guild guild = event.getJDA().getGuildById(supportID);
        botRole = guild.getRoleById(roleID);
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getUser().getIdLong() == 417349274481721345L)
            if (event.getGuild().getIdLong() != supportID)
                userUpdate(event.getJDA(), event.getUser(),event.getGuild(),true);
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
            if (event.getUser().getIdLong() == 417349274481721345L)
                if (event.getGuild().getIdLong() != supportID)
                    userUpdate(event.getJDA(), event.getUser(),event.getGuild(),false);
    }


    private void userUpdate(JDA api, User user,Guild server,boolean join) {
        Member member = api.getGuildById(supportID).getMemberById(user.getIdLong());
        if (member == null)
            return;
        List<Role> roles = new ArrayList<>(3);


        if(server.getMembers().stream().map(Member::getUser).map(User::getName).anyMatch(name -> name.equals("Accountant"))) {
            if (join) {
                while (member.getRoles().stream().map(Role::getIdLong).noneMatch(id -> id == 491954111953108992L))
                    Thread.yield();
                roles.add(server.getRoleById(491954111953108992L));
            }
        }

        if(server.getMembers().stream().map(Member::getUser).map(User::getName).anyMatch(name -> name.equals("Emoji-er"))) {
            if (join) {
                while (member.getRoles().stream().map(Role::getIdLong).noneMatch(id -> id == 491954024367652867L))
                    Thread.yield();
                roles.add(server.getRoleById(491954024367652867L));
            }
        }

        boolean isUser = api.getMutualGuilds(member.getUser()).stream().anyMatch(guild -> guild.getIdLong() != supportID);

        boolean hasrole = member.getRoles().contains(botRole);

        if (isUser && !hasrole) {
            roles.add(botRole);
            api.getGuildById(supportID).getController().addRolesToMember(member, roles).reason("guild join").complete();
        } else if (hasrole && !isUser) {
            api.getGuildById(supportID).getController().removeRolesFromMember(member, botRole).reason("guild leave").complete();
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
            if (false) {
                event.getGuild().getMembers().forEach(member -> userUpdate(event.getJDA(), member.getUser(),event.getGuild(),true));
            }
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
            if (false) {
                event.getGuild().getMembers().forEach(member -> userUpdate(event.getJDA(), member.getUser(), event.getGuild(), false));
            }
    }

    public SupportListener(long roleID) {
        this.roleID = roleID;
    }
}
