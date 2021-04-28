package com.roleGroup;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.DataOutputStream;
import java.net.Socket;

@SuppressWarnings("Duplicates")
public class SupportListener extends ListenerAdapter {
    static final long supportID = 428163245753499653L;
    
    private Role botRole;

    private final long roleID;

    @Override
    public void onReady(ReadyEvent event) {
        Guild guild = event.getJDA().getGuildById(supportID);
        botRole = guild.getRoleById(roleID);
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getGuild().getIdLong() != supportID)
            userUpdate(event.getJDA(), event.getUser());
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        if (event.getGuild().getIdLong() != supportID)
            userUpdate(event.getJDA(), event.getUser());
    }


    private void userUpdate(JDA api, User user) {
        if(user.isBot())
            return;
        Member member = api.getGuildById(supportID).getMemberById(user.getIdLong());
        if (member == null)
            return;

        boolean isUser = api.getMutualGuilds(member.getUser()).stream().anyMatch(guild -> guild.getIdLong() != supportID);

        boolean hasrole = member.getRoles().contains(botRole);

        if (isUser && !hasrole) {
            sendAction(user.getId() + " add rolegroup");
        } else if (hasrole && !isUser) {
            sendAction(user.getId() + " remove rolegroup");
        }
    }

    private void sendAction(String action){
        try {
            Socket clientSocket = new Socket(System.getenv("SUPPORT_IP"), 23445);
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            outToServer.writeBytes(action);
            clientSocket.close();
            System.out.println("sending: "+ action);
        } catch (Exception ignored) {
            System.out.println("Execption on sending: "+ action);
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
                event.getGuild().getMembers().stream().filter(m->!m.getUser().isBot()).forEach(member -> userUpdate(event.getJDA(), member.getUser()));
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
                event.getGuild().getMembers().stream().filter(m->!m.getUser().isBot()).forEach(member -> userUpdate(event.getJDA(), member.getUser()));
    }

    public SupportListener(long roleID) {
        this.roleID = roleID;
    }
}
