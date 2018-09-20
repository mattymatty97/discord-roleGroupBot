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

import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
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
            if (event.getGuild().getIdLong() != supportID)
                userUpdate(event.getJDA(), event.getUser());
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
                if (event.getGuild().getIdLong() != supportID)
                    userUpdate(event.getJDA(), event.getUser());
    }


    private void userUpdate(JDA api, User user) {
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
            outToServer.writeBytes(action+'n');
            clientSocket.close();
            System.out.println("sending: "+ action);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
                event.getGuild().getMembers().forEach(member -> userUpdate(event.getJDA(), member.getUser()));
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
                event.getGuild().getMembers().forEach(member -> userUpdate(event.getJDA(), member.getUser()));
    }

    public SupportListener(long roleID) {
        this.roleID = roleID;
    }
}
