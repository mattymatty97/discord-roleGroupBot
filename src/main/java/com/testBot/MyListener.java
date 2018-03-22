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

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
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
                    channel.sendMessage("Pong!").queue(); // Important to call .queue() on the RestAction returned by sendMessage(...)
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
                                        channel.sendMessage("Role added!").queue();
                                    } else {
                                        System.out.println("modrole syntax in guild: '" + guildname + "'");
                                        channel.sendMessage("wrong syntax!").queue();
                                    }
                                    break;
                                case "remove":
                                    //if there is a mentioned user
                                    if (mentions.size() == 1) {
                                        //call class method to remove roles
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
                                    //list all modroles
                                    System.out.println("listing modroles in guild: '" + guildname + "'");
                                    StringBuilder text = new StringBuilder("Active ModRoles:\n");
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
                                    //if there is a mention
                                    if (mentions.size() == 1) {
                                        //test if your higher role is higher than the one you're setting
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
                        break;
                    } else {
                        System.out.println("missing permissions for '" + member.getEffectiveName() + "' in guild: '" + guildname + "'");
                        channel.sendMessage("Error you have not permission to do this!").queue();
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
                                            channel.sendMessage("rolegroup created!\ntype: LIST").queue();
                                        } else {
                                            System.out.println("found existent rolegroup in guild: '" + guildname + "'");
                                            channel.sendMessage("this rolegroup already exists").queue();
                                        }
                                    }else{
                                        System.out.println("wrong syntax in guild : '" + guildname + "'");
                                        channel.sendMessage("wrong syntax").queue();
                                    }
                                }else
                                {
                                    System.out.println("wrong syntax in guild: '" + guildname + "'");
                                    channel.sendMessage("no roles or too many roles mentioned").queue();
                                }
                                break;
                            case "delete":
                                //if there is an arg
                                if (args[2] != null) {
                                    //call the class method
                                    if (guild.removeRoleGroup(args[2]) != null) {
                                        System.out.println("deleted rolegroup '" + args[2] + "' in guild: '" + guildname + "'");
                                        channel.sendMessage("rolegroup deleted").queue();
                                    } else {
                                        System.out.println("found unexistent rolegroup in guild: '" + guildname + "'");
                                        channel.sendMessage("this rolegroup does not exists").queue();
                                    }
                                }
                                break;
                            case "list": {
                                StringBuilder str = new StringBuilder("Listing rolegroups:\n");
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
                    }//*/

//----------------------------------CUSTOM COMMANDS-------------------------------
                default :
                {
                    RoleGroup group = RoleGroup.findGroup(guild.getRoleGroups(),args[0]);
                    if(group!=null)
                    {
                        if(memberHasRole(member,group.getBoundRole()))
                        {
                            if(args[1]!=null) {
                                String ret = group.command(event.getGuild(), member, args[1]);
                                channel.sendMessage(ret).queue();
                                System.out.println(" in guild: '" + guildname + "'");
                            }
                        }else{
                            channel.sendMessage("you cant do that").queue();
                            System.out.println("grouproles cc - denied in guild: '" + guildname + "'");
                        }
                    }else{
                        channel.sendMessage("unknown command").queue();
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

    private boolean memberHasRole(Member member,Long roleId)
    {
        List<Role> list = member.getRoles();
        for (Role role : list)
        {
            if(roleId.equals(role.getIdLong()))
                return true;
        }
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
        EmbedBuilder message = new EmbedBuilder();

        message.setColor(Color.GREEN);

        if(args.length == 1) {
            message.setTitle("Help for testbot:");
            message.addField("help", "shows this help", false);
            message.addField("ping", "answers pong (userfull for speed tests and alive check)", false);

            //if is allowed to use mod commands
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

                message.addField("rolegroup", "set a coustom command controlled by a trigger role\n" +
                        "that allows users to add or remove themself to a list of roles\n" +
                        "Usage: complex call **help rolegroup** ", false);
            }
            message.addBlankField(false);
            message.addField("CUSTOM COMMANDS:", "up now\n they are called rolegroups", false);
        }else
            switch (args[1])
            {
                case "ping":
                    message.setTitle("testbot help for ping:");
                    message.addField("YOU REALLY NEED HELP ON THIS?", "", false);
                    break;
                case "help":
                    message.setTitle("testbot help for help:");
                    message.addField("SERIOUSLY?", "", false);

                case "prefix":
                    message.setTitle("testbot help for prefix:");
                    message.setDescription("allows mods to modify bot prefix");
                    message.addField("Usage:", "prefix [new prefix]", false);
                    message.addField("Example:", "prefix tb!\nprefix t?\nprefix bot!", false);
                    message.addField("Suggestions:", "use as last charachter one of this |!?", false);

                    break;

                case "modrole":
                    message.setTitle("testbot help for modrole:");
                    message.setDescription("allows mods to manage roles that are allowed to access mod commands");
                    message.addField("Usage:", "modrole <action> [RoleMention]", false);
                    message.addField("Actions:", "add: adds the mentioned role\n"+
                                                            "remove: removes the mentioned role\n"+
                                                            "list: prints all active roles", false);
                    message.addField("Example:", "modrole add @Mod\nmodrole remove @User\nmodrole list", false);
                    message.addField("do you know?", "owner is always allowed to use mod commands", false);
                    break;

                case "role":
                    message.setTitle("testbot help for role:");
                    message.setDescription("allows mods to manage they own roles");
                    message.addField("Usage:", "role <action> [RoleMention]", false);
                    message.addField("Actions:", "add: adds the mentioned role\n"+
                            "remove: removes the mentioned role\n", false);
                    message.addField("Example:", "role add @Strong\nmodrole remove @Weak", false);
                    message.addField("do you know?", "administrators are always able to modify\n"+
                                                                "every role and user in theyre guild", false);

                    break;
                case "rolegroup":
                    message.setTitle("testbot help for rolegroups:");
                    message.setDescription("set a coustom command controlled by a trigger role\n"+
                                            "that allows users to add or remove themself to a list of roles");
                    message.addField("Usage:", "rolegroup create [command] [RoleMention]\n"+
                            "creates the command [command] triggered by mentioned role\n\n"+
                            "rolegroup delete [command]\n"+
                            "deletes the specified command\n\n"+
                            "rolegroup list\n" +
                            "lists all existents rolegroups\n\n" +
                            "rolegroup [command] add [RoleMention] as [nick]\n" +
                            "adds the mentioned role to [command] and sets his trigger name as [nick]\n\n" +
                            "rolegroup [command] remove [nick]\n" +
                            "removes the Role whit trigger name [nick] from [command] role list\n\n" +
                            "rolegroup [command] list\n" +
                            "lists all connected Roles and theyre nicks\n\n" +
                            "rolegroup [command] type <type>\n" +
                            "sets the type of command", false);
                    message.addField("Types:", "LIST\n" +
                            "a list of roles all indipendent\n\n" +
                            "other coming soon", false);
                    message.addField("Example:", "rolegroup create color @painter\n" +
                            "rolegroup delete pirate\n" +
                            "rolegroup color add @yellow_role as yellow\n" +
                            "rolegroup color remove blue\n" +
                            "rolegroup type LIST", false);
                    message.addField("do you know?", "all variables [variable] are case sensitive\n" +
                            "that means that 'Case' is different from 'case'", false);
                default :
                    message.setTitle("default help of testbot:");
                    message.setDescription("I don't know the command you asked.... try again?");
            }
        channel.sendMessage(message.build()).queue();
    }


    MyListener(Connection conn, List<BotGuild> savedGuilds) {
        this.conn = conn;
        this.savedGuilds = savedGuilds;
    }
}
