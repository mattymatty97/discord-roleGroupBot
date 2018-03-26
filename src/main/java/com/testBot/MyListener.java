package com.testBot;


import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.role.RoleDeleteEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.awt.*;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class MyListener extends ListenerAdapter {
    private Connection conn;
    private List<BotGuild> savedGuilds;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        //locales generation (dynamic strings from file selectionable by language)
        ResourceBundle output;
        //if is a direct message exit immediately
        if(event.isFromType(ChannelType.PRIVATE)) return;
        //if is a bot exit immediately
        if (event.getAuthor().isBot()) return;
        BotGuild guild;
        //name of sender server
        String guildname = event.getGuild().getName();
        //search for existent informations class for server
        guild = findGuild(event.getGuild().getIdLong());
        if (guild == null) {
            //create local instance of server informations
            guild = new BotGuild(event.getGuild().getIdLong(), guildname.intern(), conn);
            savedGuilds.add(guild);
        }
        //set locales to giuld setting

        output= guild.getMessages();

        //get sender member
        Member member = event.getMember();
        //get channel to send
        MessageChannel channel = event.getChannel();
        //get message
        Message message = event.getMessage();
        //get text
        String content = message.getContent();
        System.out.println("Message from '" + member.getEffectiveName() + "' in guild '" + guildname + "'");
        //if length is enough test if the message starts with right prefix
        if (content.length() > guild.getPrefix().length() && content.substring(0, guild.getPrefix().length()).equals(guild.getPrefix())) {
            //split by spaces into args
            String[] args = content.substring(guild.getPrefix().length()).split(" +");
            //test first argument
            switch (args[0]) {

//------USER---------------------HELP--------------------------------------

                case "help":
                    System.out.println("help shown in guild: '" + guildname + "'");
                    PrintHelp(channel, member, guild,args);
                    break;

//------USER--------------------PING---------------------------------------

                case "ping":
                case "Ping":
                    System.out.println("Ping executed in guild: '" + guildname + "'");
                    channel.sendMessage(output.getString("pong")).queue(); // Important to call .queue() on the RestAction returned by sendMessage(...)
                    break;

//------MOD---------------------SET----------------------------------------

                case "prefix":
                    //if member is allowed
                    if (member.isOwner() || guild.memberIsMod(member)) {
                        //if there is a second arg
                        if (args[1] != null) {
                            //if is not too long
                            if (args[1].length() > 10) {
                                System.out.println("prefix set failed in guild: '" + guildname + "'");
                                channel.sendMessage(output.getString("error-long-prefix")).queue();
                                break;
                            }
                            System.out.println("seting prefix for guild: '" + guildname + "' to: '" + args[1]);
                            guild.setPrefix(args[1]);
                            channel.sendMessage(output.getString("prefix-correct")).queue();
                        }
                        break;
                    } else {
                        channel.sendMessage(output.getString("error-user-permission")).queue();
                        System.out.println("no permission in guild: '" + guildname + "'");
                    }

                    break;


//-------MOD-------------------MODROLE-------------------------------------

                case "modrole":
                    //if member is allowed
                    if (member.isOwner() || guild.memberIsMod(member)) {
                        //if there are other arguments
                        if (args[1] != null) {
                            //get mentioned roles
                            List<Role> mentions = message.getMentionedRoles();
                            //test on second arg
                            switch (args[1]) {
                                case "add":
                                    //if there is a mentioned role
                                    if (mentions.size() == 1) {
                                        //call class method to add roles
                                        System.out.println("adding modrole '" + mentions.get(0).getName() + "' to guild '" + guildname + "'");
                                        guild.addModRole(mentions.get(0).getIdLong(), mentions.get(0).getName());
                                        channel.sendMessage(output.getString("modrole-added")).queue();
                                    } else {
                                        System.out.println("modrole syntax in guild: '" + guildname + "'");
                                        channel.sendMessage(output.getString("error-wrong-syntax")).queue();
                                    }
                                    break;
                                case "remove":
                                    //if there is a mentioned user
                                    if (mentions.size() == 1) {
                                        //call class method to remove roles
                                        System.out.println("removing modrole '" + mentions.get(0).getName() + "' from guild '" + guildname + "'");
                                        if (guild.removeModRole(mentions.get(0).getIdLong()) != null)
                                            channel.sendMessage(output.getString("modrole-removed")).queue();
                                        else
                                            channel.sendMessage(output.getString("error-not-modrole")).queue();
                                    } else {
                                        System.out.println("modrole syntax in guild: '" + guildname + "'");
                                        channel.sendMessage(output.getString("error-wrong-syntax")).queue();
                                    }
                                    break;
                                case "list":
                                    //list all modroles
                                    System.out.println("listing modroles in guild: '" + guildname + "'");
                                    StringBuilder text = new StringBuilder(output.getString("modrole-list")+"\n");
                                    for (Long id : guild.getModRolesById()) {
                                        for (Role role : event.getGuild().getRoles()) {
                                            if (role.getIdLong() == (id))
                                                //gets the role name whit api method
                                                text.append(role.getName()).append("\n");
                                        }
                                    }
                                    channel.sendMessage(text.toString()).queue();
                                    break;
                                default:
                                    System.out.println("command syntax in guild: '" + guildname + "'");
                                    channel.sendMessage(output.getString("error-wrong-syntax")).queue();
                            }

                        }
                        break;
                    } else {
                    channel.sendMessage(output.getString("error-user-permission")).queue();
                    System.out.println("no permission in guild: '" + guildname + "'");
                }
                    break;


//-------MOD-----------------------ROLE------------------------------------------------

                case "role":
                    //if the member is allowed
                    if (member.isOwner() || guild.memberIsMod(member)) {
                            //get mentions list
                            List<Role> mentions = message.getMentionedRoles();
                            switch (args[1]) {
                                case "add":
                                    //if there is a mention
                                    if (mentions.size() == 1) {
                                        //test if your higher role is higher than the one you're setting
                                        List<Role> roles = event.getGuild().getSelfMember().getRoles();
                                        if (roles.get(0).getPosition() > mentions.get(0).getPosition()) {
                                            //add role using api method
                                            if(!memberHasRole(member,mentions.get(0).getIdLong())) {
                                                event.getGuild().getController().addRolesToMember(member, mentions).queue();
                                                channel.sendMessage(output.getString("role-added")).queue();
                                                System.out.println("added a role to '" + member.getEffectiveName() + "'in guild: '" + guildname + "'");
                                            }else
                                            {
                                                channel.sendMessage(output.getString("error-role-owned")).queue();
                                                System.out.println("role error in guild: '" + guildname + "'");
                                            }
                                        } else {
                                            System.out.println("role permission error in guild : '" + guildname + "'");
                                            channel.sendMessage(output.getString("error-bot-permission")).queue();
                                        }
                                    } else {
                                        System.out.println("wrong role syntax in guild: '" + guildname + "'");
                                        channel.sendMessage(output.getString("error-wrong-syntax")).queue();
                                    }
                                    break;
                                case "remove":
                                    //if there is a mention
                                    if (mentions.size() == 1) {
                                        //test if your higher role is higher than the one you're setting
                                        List<Role> roles = event.getGuild().getSelfMember().getRoles();
                                        if (roles.get(0).getPosition() > mentions.get(0).getPosition()) {
                                            if(memberHasRole(member,mentions.get(0).getIdLong())) {
                                                event.getGuild().getController().removeRolesFromMember(member, mentions).queue();
                                                channel.sendMessage(output.getString("role-removed")).queue();
                                                System.out.println("removed a role to '" + member.getEffectiveName() + "'in guild: '" + guildname + "'");
                                            }else
                                            {
                                                channel.sendMessage(output.getString("error-role-not-owned")).queue();
                                                System.out.println("role error in guild: '" + guildname + "'");
                                            }
                                        } else {
                                            System.out.println("role permission error in guild : '" + guildname + "'");
                                            channel.sendMessage(output.getString("error-bot-permission")).queue();
                                        }
                                    } else {
                                        System.out.println("wrong role syntax in guild: '" + guildname + "'");
                                        channel.sendMessage(output.getString("error-wrong-syntax")).queue();
                                    }
                                    break;
                                default:
                                    channel.sendMessage(output.getString("error-wrong-syntax")).queue();
                            }
                        break;
                    } else {
                        channel.sendMessage(output.getString("error-user-permission")).queue();
                        System.out.println("no permission in guild: '" + guildname + "'");
                    }
                    break;

//-------MOD--------------------------ROLEGROUP-------------------------------

                case "rolegroup":
                    //if member is allowed
                    if (member.isOwner() || guild.memberIsMod(member)) {
                        switch (args[1]) {
                            case "create":
                                //get a list of all mentions
                                List<Role> list = message.getMentionedRoles();
                                //if there is a mentioned role
                                if (list.size() == 1) {
                                    //if the argument is not the mentioned role
                                    if (!args[2].contains(list.get(0).getName())) {
                                        //call the class method
                                        if (guild.addRoleGroup(list.get(0), args[2]) != null) {
                                            System.out.println("created rolegroup '" + args[2] + "' in guild: '" + guildname + "'");
                                            channel.sendMessage(output.getString("rolegroup-created")).queue();
                                        } else {
                                            System.out.println("found existent rolegroup in guild: '" + guildname + "'");
                                            channel.sendMessage(output.getString("error-existing-rolegroup")).queue();
                                        }
                                    }else{
                                        System.out.println("wrong syntax in guild : '" + guildname + "'");
                                        channel.sendMessage(output.getString("error-wrong-syntax")).queue();
                                    }
                                }else
                                {
                                    System.out.println("wrong syntax in guild: '" + guildname + "'");
                                    channel.sendMessage(output.getString("error-role-mention")).queue();
                                }
                                break;
                            case "delete":
                                //if there is an arg
                                if (args[2] != null) {
                                    //call the class method
                                    if (guild.removeRoleGroup(args[2]) != null) {
                                        System.out.println("deleted rolegroup '" + args[2] + "' in guild: '" + guildname + "'");
                                        channel.sendMessage(output.getString("rolegroup-deleted")).queue();
                                    } else {
                                        System.out.println("found unexistent rolegroup in guild: '" + guildname + "'");
                                        channel.sendMessage(output.getString("error-rolegroup-not-exist")).queue();
                                    }
                                }
                                break;
                            case "list": {
                                StringBuilder str = new StringBuilder(output.getString("rolegroup-listing")).append("\n");
                                for (RoleGroup group : guild.getRoleGroups()) {
                                    str.append(group.getGroupName()).append("\n");
                                }
                                channel.sendMessage(str.toString()).queue();
                                System.out.println("listing rolegroups in guild: '" + guildname + "'");
                            }
                                break;
                            default:
                                    guild.optionRoleGroup(args[1], Arrays.copyOfRange(args, 2, args.length),message, channel);
                                    System.out.println(" in guild: '" + guildname + "'");
                                break;
                        }
                        break;
                    }else {
                        channel.sendMessage(output.getString("error-user-permission")).queue();
                        System.out.println("no permission in guild: '" + guildname + "'");
                    }

//----------------------------------CUSTOM COMMANDS-------------------------------
                default :
                {
                    RoleGroup group = RoleGroup.findGroup(guild.getRoleGroups(),args[0]);
                    if(group!=null)
                    {
                        if(memberHasRole(member,group.getBoundRole()))
                        {
                            if(args[1]!=null) {
                                String ret = group.command(event.getGuild(), member, args[1],output);
                                channel.sendMessage(ret).queue();
                                System.out.println(" in guild: '" + guildname + "'");
                            }
                        }else{
                            channel.sendMessage(output.getString("error-user-permission")).queue();
                            System.out.println("rolegroup cc - denied in guild: '" + guildname + "'");
                        }
                    }else{
                        channel.sendMessage(output.getString("error-unknown-command")).queue();
                        System.out.println("not a command");
                    }
                }
            }

//-------ALL---------------------------IGNORED--------------------------------

        } else {
            //if the message was not directed to the bot
            System.out.println("Ignored");
        }
    }

    @Override
    public void onRoleDelete(RoleDeleteEvent event) {
        ResourceBundle output;
        BotGuild guild;
        //name of sender server
        String guildname = event.getGuild().getName();
        //search for existent informations class for server
        guild = findGuild(event.getGuild().getIdLong());
        if (guild == null) {
            //create local instance of server informations
            guild = new BotGuild(event.getGuild().getIdLong(), guildname.intern(), conn);
            savedGuilds.add(guild);
        }
        //set locales to giuld setting

        output= guild.getMessages();

        if(guild.onRoleDeleted(event.getRole()))
        {
            System.out.println("role deleted in guild: " +guildname);
        }

    }

    private boolean memberHasRole(Member member, Long roleId)
    {
        List<Role> list = member.getRoles();
        Role role = member.getGuild().getRoleById(roleId);
        if(role!=null)
            if(list.contains(role))
                return true;
        return false;
    }


    //need explanation?
    private BotGuild findGuild(Long guildId) {
        for (BotGuild guild : savedGuilds) {
            if (guild.getId().equals(guildId))
                return guild;
        }
        return null;
    }

    //prints the help message
    private void PrintHelp(MessageChannel channel, Member member, BotGuild guild, String[] args) {
        ResourceBundle output = guild.getMessages();
        EmbedBuilder helpMsg = new EmbedBuilder();
        StringBuilder str = new StringBuilder();
        helpMsg.setColor(Color.GREEN);

        if(args.length == 1) {
            //help is dynamic (different for every user)
            helpMsg.setTitle(output.getString("help-title"));
            helpMsg.addField("help", output.getString("help-def-help"), false);
            helpMsg.addField("ping", output.getString("help-def-ping"), false);

            //if is allowed to use mod commands
            if (member.isOwner() || guild.memberIsMod(member)) {
                helpMsg.addBlankField(false);
                helpMsg.addField("MOD commands:", "", false);
                helpMsg.addField("prefix", output.getString("help-def-prefix"), false);

                helpMsg.addField("modrole", output.getString("help-def-modrole"), false);

                helpMsg.addField("role", output.getString("help-def-role"), false);

                helpMsg.addField("rolegroup", output.getString("help-def-rolegroup"), false);
            }
            helpMsg.addBlankField(false);
            for(RoleGroup roleGroup : guild.getRoleGroups())
            {
                if(memberHasRole(member,roleGroup.getBoundRole()) && roleGroup.isValid())
                {
                    str.append("\n");
                    str.append(roleGroup.printHelp());
                    str.append("\n");
                }
            }
            helpMsg.addField("CUSTOM COMMANDS:", str.toString(), false);
            helpMsg.setFooter(output.getString("help-footer"),null);
        }else
            switch (args[1])
            {
                case "ping":
                    helpMsg.setTitle(output.getString("help-ping-title"));
                    helpMsg.addField(output.getString("help-ping"), "", false);
                    break;
                case "help":
                    helpMsg.setTitle(output.getString("help-help-title"));
                    helpMsg.addField(output.getString("help-help"), "", false);
                    break;

                case "prefix":
                    helpMsg.setTitle(output.getString("help-prefix-title"));
                    helpMsg.setDescription(output.getString("help-prefix-description"));
                    helpMsg.addField(output.getString("help-field-usage"), output.getString("help-prefix-usage"), false);
                    helpMsg.addField(output.getString("help-field-example"), output.getString("help-prefix-example"), false);
                    helpMsg.addField(output.getString("help-field-suggestions"), output.getString("help-prefix-suggestions"), false);
                    break;

                case "modrole":
                    helpMsg.setTitle(output.getString("help-modrole-title"));
                    helpMsg.setDescription(output.getString("help-modrole-description"));
                    helpMsg.addField(output.getString("help-field-usage"), output.getString("help-modrole-usage"), false);
                    helpMsg.addField(output.getString("help-field-actions"), output.getString("help-modrole-actions"), false);
                    helpMsg.addField(output.getString("help-field-example"), output.getString("help-modrole-examples"), false);
                    helpMsg.addField(output.getString("help-field-dyk"), output.getString("help-modrole-dyk"), false);
                    break;

                case "role":
                    helpMsg.setTitle(output.getString("help-role-title"));
                    helpMsg.setDescription(output.getString("help-role-description"));
                    helpMsg.addField(output.getString("help-field-usage"), output.getString("help-role-usage"), false);
                    helpMsg.addField(output.getString("help-field-actions"), output.getString("help-role-actions"), false);
                    helpMsg.addField(output.getString("help-field-example"), output.getString("help-role-example"), false);
                    helpMsg.addField(output.getString("help-field-dyk"), output.getString("help-role-dyk"), false);

                    break;
                case "rolegroup":
                    helpMsg.setTitle(output.getString("help-rolegroup-title"));
                    helpMsg.setDescription(output.getString("help-rolegroup-description"));
                    helpMsg.addField(output.getString("help-field-usage"), output.getString("help-rolegroup-usage"), false);
                    helpMsg.addField(output.getString("help-field-types"), output.getString("help-rolegroup-types"), false);
                    helpMsg.addField(output.getString("help-field-example"), output.getString("help-rolegroup-example"), false);
                    helpMsg.addField(output.getString("help-field-user-example"), output.getString("help-rolegroup-user-example"), false);
                    helpMsg.addField(output.getString("help-field-dyk"), output.getString("help-rolegroup-dyk"), false);
                    break;
                default :
                    helpMsg.setTitle(output.getString("help-404-title"));
                    helpMsg.setDescription(output.getString("help-404-cmd"));
            }
        channel.sendMessage(helpMsg.build()).queue();
    }


    public MyListener(Connection conn, List<BotGuild> savedGuilds) {
        this.conn = conn;
        this.savedGuilds = savedGuilds;
    }
}
