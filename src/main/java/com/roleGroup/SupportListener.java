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
                try {
                    Thread.sleep(3000);
                    userUpdate(event.getJDA(), event.getUser());
                }catch (InterruptedException ignored){}
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
            if (event.getUser().getIdLong() == 417349274481721345L)
                if (event.getGuild().getIdLong() != supportID)
                    try {
                        Thread.sleep(3000);
                        userUpdate(event.getJDA(), event.getUser());
                    }catch (InterruptedException ignored){}
    }


    private void userUpdate(JDA api, User user) {
        Member member = api.getGuildById(supportID).getMemberById(user.getIdLong());
        if (member == null)
            return;
        
        boolean isUser = api.getMutualGuilds(member.getUser()).stream().anyMatch(guild -> guild.getIdLong() != supportID);

        boolean hasrole = member.getRoles().contains(botRole);

        if (isUser && !hasrole) {
            api.getGuildById(supportID).getController().addRolesToMember(member, botRole).reason("guild join").complete();
        } else if (hasrole && !isUser) {
            api.getGuildById(supportID).getController().removeRolesFromMember(member, botRole).reason("guild leave").complete();
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        try {
            Thread.sleep(3000*event.getGuild().getMembers().size());
            if (false) {
                event.getGuild().getMembers().forEach(member -> userUpdate(event.getJDA(), member.getUser()));
            }
        }catch (InterruptedException ignored){}
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        try {
            Thread.sleep(3000*event.getGuild().getMembers().size());
            if (false) {
                event.getGuild().getMembers().forEach(member -> userUpdate(event.getJDA(), member.getUser()));
            }
        }catch (InterruptedException ignored){}
    }

    public SupportListener(long roleID) {
        this.roleID = roleID;
    }
}
