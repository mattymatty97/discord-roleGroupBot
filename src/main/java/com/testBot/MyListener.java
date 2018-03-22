package com.testBot;


import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.awt.*;
import java.sql.*;

import java.util.Arrays;
import java.util.List;

public class MyListener extends ListenerAdapter {
    private Connection conn;
    private List<BotGuild> savedGuilds;

    private static String toSendUser =
            "help of testbot\n" +
                    "- ping: answers Pong!";
    private static String toAddMod = "\n\n" +
            "mod commands:\n" +
            "- prefix [prefix]: changes bot prefix for this server\n" +
            "- modrole <add/remove/list> [roleMention]: add a role to modroles\n" +
            "- role <add/remove> [rolemention] : add yourself a role";

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        BotGuild guild;
        String guildname = event.getGuild().getName();
        guild = findGuild(event.getGuild().getIdLong());
        if (guild == null) {
            guild = new BotGuild(event.getGuild().getIdLong(), guildname.intern(), conn);
            savedGuilds.add(guild);
        }
        if (event.getAuthor().isBot()) return;
        Member member = event.getMember();
        MessageChannel channel = event.getChannel();
        Message message = event.getMessage();
        String content = message.getContent();
        System.out.println("Message from '" + member.getEffectiveName() + "' in guild '" + guildname + "'");
        if (content.length() > guild.getPrefix().length() && content.substring(0, guild.getPrefix().length()).equals(guild.getPrefix())) {
            String[] args = content.substring(guild.getPrefix().length()).split(" ");
            switch (args[0]) {

//------USER---------------------HELP--------------------------------------

                case "help":
                    System.out.println("help shown in guild: '" + guildname + "'");
                    /*channel.sendMessage(toSendUser).queue();
                    if (member.isOwner() || guild.memberIsMod(member))
                        channel.sendMessage(toAddMod).queue();*/
                    PrintHelp(channel, member, guild);
                    break;

//------USER--------------------PING---------------------------------------

                case "ping":
                case "Ping":
                    System.out.println("Ping executed in guild: '" + guildname + "'");
                    channel.sendMessage("Pong!").queue(); // Important to call .queue() on the RestAction returned by sendMessage(...)
                    break;

//------MOD---------------------SET----------------------------------------

                case "prefix":
                    if (member.isOwner() || guild.memberIsMod(member)) {
                        if (args[1] != null) {
                            if (args[1].length() > 10) {
                                System.out.println("prefix set failed in guild: '" + guildname + "'");
                                channel.sendMessage("Error too long prefix (limit is 10)!").queue();
                                break;
                            }
                            System.out.println("seting prefix for guild: '" + guildname + "' to: '" + args[1] + "?");
                            guild.setPrefix(args[1]);
                            channel.sendMessage("Prefix set! in guild: '" + guildname + "'").queue();
                        }
                        break;
                    } else {
                        channel.sendMessage("Error you have not permission to do this!").queue();
                        System.out.println("no permission in guild: '" + guildname + "'");
                    }

                    break;


//-------MOD-------------------MODROLE-------------------------------------

                case "modrole":
                    if (member.isOwner() || guild.memberIsMod(member)) {
                        if (args[1] != null) {
                            List<Role> mentions = message.getMentionedRoles();
                            switch (args[1]) {
                                case "add":
                                    if (mentions.size() == 1) {
                                        System.out.println("adding modrole '" + mentions.get(0).getName() + "' to guild '" + guildname + "'");
                                        guild.addModRole(mentions.get(0).getIdLong(), mentions.get(0).getName());
                                        channel.sendMessage("Role added!").queue();
                                    } else {
                                        System.out.println("modrole syntax in guild: '" + guildname + "'");
                                        channel.sendMessage("wrong syntax!").queue();
                                    }
                                    break;
                                case "remove":
                                    if (mentions.size() == 1) {
                                        System.out.println("removing modrole '" + mentions.get(0).getName() + "' from guild '" + guildname + "'");
                                        if (guild.removeModRole(mentions.get(0).getIdLong()) != null)
                                            channel.sendMessage("Role removed!").queue();
                                        else
                                            channel.sendMessage("Role is not a modrole!").queue();
                                    } else {
                                        System.out.println("modrole syntax in guild: '" + guildname + "'");
                                        channel.sendMessage("wrong syntax!").queue();
                                    }
                                    break;
                                case "list":
                                    System.out.println("listing modroles in guild: '" + guildname + "'");
                                    StringBuilder text = new StringBuilder("Active ModRoles:\n");
                                    for (Long id : guild.getModRolesById()) {
                                        for (Role role : event.getGuild().getRoles()) {
                                            if (role.getIdLong() == (id))
                                                text.append(role.getName()).append("\n");
                                        }
                                    }
                                    channel.sendMessage(text.toString()).queue();
                                    break;
                                default:
                                    System.out.println("command syntax in guild: '" + guildname + "'");
                                    channel.sendMessage("wrong syntax!").queue();
                            }

                        }
                        break;
                    } else {
                        System.out.println("no permission in guild: '" + guildname + "'");
                        channel.sendMessage("Error you have not permission to do this!").queue();
                    }
                    break;


//-------MOD-----------------------ROLE------------------------------------------------

                case "role":
                    if (member.isOwner() || guild.memberIsMod(member)) {
                        if (args[1] != null) {
                            List<Role> mentions = message.getMentionedRoles();
                            switch (args[1]) {
                                case "add":
                                    if (mentions.size() == 1) {
                                        List<Role> roles = event.getGuild().getSelfMember().getRoles();
                                        if (roles.get(0).getPosition() > mentions.get(0).getPosition()) {
                                            event.getGuild().getController().addRolesToMember(member, mentions).queue();
                                            channel.sendMessage("Role added!").queue();
                                            System.out.println("added a role to '" + member.getEffectiveName() + "'in guild: '" + guildname + "'");
                                        } else {
                                            System.out.println("role permission error in guild : '" + guildname + "'");
                                            channel.sendMessage("Cannot modify a higher or equal role to my higher role!").queue();
                                        }
                                    } else {
                                        System.out.println("wrong role syntax in guild: '" + guildname + "'");
                                        channel.sendMessage("wrong syntax!").queue();
                                    }
                                    break;
                                case "remove":
                                    if (mentions.size() == 1) {
                                        List<Role> roles = event.getGuild().getSelfMember().getRoles();
                                        if (roles.get(0).getPosition() > mentions.get(0).getPosition()) {
                                            event.getGuild().getController().removeRolesFromMember(member, mentions).queue();
                                            channel.sendMessage("Role removed!").queue();
                                            System.out.println("removed a role to '" + member.getEffectiveName() + "'in guild: '" + guildname + "'");
                                        } else {
                                            System.out.println("role permission error in guild : '" + guildname + "'");
                                            channel.sendMessage("Cannot modify a higher or equal role to my higher role!").queue();
                                        }
                                    } else {
                                        System.out.println("wrong role syntax in guild: '" + guildname + "'");
                                        channel.sendMessage("wrong syntax!").queue();
                                    }
                                    break;
                                default:
                                    channel.sendMessage("wrong syntax!").queue();
                            }

                        }
                        break;
                    } else {
                        System.out.println("missing permissions for '" + member.getEffectiveName() + "' in guild: '" + guildname + "'");
                        channel.sendMessage("Error you have not permission to do this!").queue();
                    }
                    break;

//-------MOD--------------------------ROLEGROUP-------------------------------
                /*
                case "rolegroup"://disabled for now
                    if (member.isOwner() || guild.memberIsMod(member)) {
                        switch (args[1]) {
                            case "create":
                                List<Role> list = message.getMentionedRoles();
                                if (list.size() != 1) {
                                    if (args[2] != null && !args[2].contains(list.get(0).getName())) {
                                        if (guild.addRoleGroup(list.get(0).getIdLong(), args[2]) != null) {
                                            System.out.println("created rolegroup '" + args[2] + "' in guild: '" + guildname + "'");
                                            channel.sendMessage("rolegroup created!\ntype: LIST").queue();
                                        } else {
                                            System.out.println("found existent rolegroup in guild: '" + guildname + "'");
                                            channel.sendMessage("this rolegroup already exists").queue();
                                        }
                                    }
                                }
                                break;
                            case "remove":
                                if (args[2] != null) {
                                    if (guild.removeRoleGroup(args[2]) != null) {
                                        System.out.println("removed rolegroup '" + args[2] + "' in guild: '" + guildname + "'");
                                        channel.sendMessage("rolegroup created!\ntype: LIST").queue();
                                    } else {
                                        System.out.println("found unexistent rolegroup in guild: '" + guildname + "'");
                                        channel.sendMessage("this rolegroup does not exists").queue();
                                    }
                                }
                                break;
                            default:
                                if (args[2] != null) {
                                    if (guild.modifyRoleGroup(args[2], Arrays.copyOfRange(args, 3, args.length), event.getChannel()) != null) {
                                        System.out.println("removed rolegroup '" + args[2] + "' in guild: '" + guildname + "'");
                                        channel.sendMessage("rolegroup created!\ntype: LIST").queue();
                                    } else {
                                        System.out.println("found unexistent rolegroup in guild: '" + guildname + "'");
                                        channel.sendMessage("this rolegroup does not exists").queue();
                                    }
                                }
                                break;

                        }
                        break;
                    }*/
            }

//-------ALL---------------------------IGNORED--------------------------------

        } else {
            System.out.println("Ignored");
        }
    }

    private BotGuild findGuild(Long guildId) {
        for (BotGuild guild : savedGuilds) {
            if (guild.getId().equals(guildId))
                return guild;
        }
        return null;
    }

    private void PrintHelp(MessageChannel channel, Member member, BotGuild guild) {
        EmbedBuilder message = new EmbedBuilder();

        message.setColor(Color.GREEN);

        message.setTitle("Help for testbot:");
        message.addField("help", "shows this help", false);
        message.addField("ping", "answers pong (userfull for speed tests and alive check)", false);

        if (member.isOwner() || guild.memberIsMod(member)) {
            message.addBlankField(false);
            message.addField("MOD commands:", "", false);
            message.addField("prefix", "sets the prefix of the bot\n" +
                    "Usage: prefix [prefix]", false);

            message.addField("modrole", "manages the roles allowed to use mod commands\n" +
                    "Usage: modrole <action> [RoleMention]\n" +
                    "Actions: add, remove, list", false);

            message.addField("role", "add or remove a roleto caller\n" +
                    "Usage: role <action> [RoleMention]\n" +
                    "actions: add, remove", false);
        }
        message.addBlankField(false);
        message.addField("COUSTOUM COMMANDS:", "coming soon", false);

        channel.sendMessage(message.build()).queue();
    }


    MyListener(Connection conn, List<BotGuild> savedGuilds) {
        this.conn = conn;
        this.savedGuilds = savedGuilds;
    }
}
