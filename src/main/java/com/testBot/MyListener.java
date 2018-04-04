package com.testBot;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.role.RoleDeleteEvent;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.utils.PermissionUtil;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.List;

public class MyListener extends ListenerAdapter {
    private Connection conn;
    private List<BotGuild> savedGuilds;
    private EmojiGuild emojiGuild;
    public static boolean deleted=false;

    @Override
    public void onReady(ReadyEvent event) {
        Statement stmt;
        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT guildid FROM guilds");
            List<Long> to_remove = new ArrayList<>();
            while (rs.next())
            {
                boolean found=false;
                for (Guild guild : event.getJDA().getSelfUser().getMutualGuilds())
                {
                    if(guild.getIdLong()==rs.getLong(1)) {
                        found = true;
                        break;
                    }
                }
                if(!found)
                    to_remove.add(rs.getLong(1));
            }
            rs.close();
            stmt.close();
            for (Long guildId : to_remove)
                guildDeleteDB(conn,guildId);
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }

        updateServerCount(event.getJDA());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        //locales generation (dynamic strings from file selectionable by language)
        ResourceBundle output;
        if(checkConnection()) {
            //if is a direct message exit immediately
            if (event.isFromType(ChannelType.PRIVATE)) return;
            //if is a bot exit immediately
            if (event.getAuthor().isBot()) return;
            //if i cant write
            if(!PermissionUtil.checkPermission(event.getTextChannel(),event.getGuild().getSelfMember(),Permission.MESSAGE_WRITE))
                return;

            BotGuild guild;
            //name of sender server
            String guildname = event.getGuild().getName();
            //search for existent informations class for server
            guild = findGuild(event.getGuild().getIdLong());
            if (guild == null) {
                //create local instance of server informations
                guild = new BotGuild(event.getGuild(), conn);
                savedGuilds.add(guild);
                output = guild.getMessages();
                if (deleted) {
                    deleted = false;
                    System.out.println("role deleted in guild: " + guildname);
                    event.getGuild().getOwner().getUser().openPrivateChannel().queue((channel) ->
                    {
                        channel.sendMessage(output.getString("event-role-deleted")).queue();
                        channel.sendMessage(output.getString("event-role-deleted-2")).queue();
                    });
                }
            } else {
                //set locales to giuld setting

                output = guild.getMessages();
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
                        PrintHelp(channel, member, guild, args);
                        break;

//------USER--------------------PING---------------------------------------

                    case "ping":
                    case "Ping":
                        System.out.println("Ping executed in guild: '" + guildname + "'");
                        channel.sendMessage(output.getString("pong")).queue(); // Important to call .queue() on the RestAction returned by sendMessage(...)
                        break;

//------MIXED------------------EMOJI--------------------------------------
                    case "emoji":
                        if(args.length >= 2)
                        {
                            switch (args[1])
                            {
                                case "list":
                                    if(args.length==3) {
                                        channel.sendMessage(output.getString("emoji-list") + emojiGuild.getEmojiList(args[2],event.getJDA())).queue();
                                        System.out.println("emoji list shown in guild: '" + guildname + "'");
                                    }else{
                                        channel.sendMessage(output.getString("error-emoji-list")).queue();
                                        System.out.println("error emoji list in guild: '" + guildname + "'");
                                    }
                                    break;

                                case "servers":
                                    String result = emojiGuild.printServers(event.getJDA());
                                    channel.sendMessage(output.getString("emoji-server-list")+result).queue();
                                    System.out.println("emoji server list shown in guild: '" + guildname + "'");
                                    break;

                                case "register":
                                    if (member.isOwner() || guild.memberIsMod(member)) {
                                        if(args.length==3){
                                            if(args[2].length()<=10){
                                                channel.sendMessage(emojiGuild.registerGuild(guild.getId(),args[2],output)).queue();
                                            }else{
                                                System.out.println("emoji register failed in guild: '" + guildname + "'");
                                                channel.sendMessage(output.getString("error-long-title")).queue();
                                            }
                                        }else{
                                            System.out.println("command syntax in guild: '" + guildname + "'");
                                            channel.sendMessage(output.getString("error-wrong-syntax")).queue();
                                        }
                                    }else {
                                        channel.sendMessage(output.getString("error-user-permission")).queue();
                                        System.out.println("no permission in guild: '" + guildname + "'");
                                    }
                                    break;

                                case "unregister":
                                    if (member.isOwner() || guild.memberIsMod(member)) {
                                        if(args.length==3){
                                            channel.sendMessage(emojiGuild.unRegisterGuild(guild.getId(),output)).queue();
                                        }else{
                                            System.out.println("command syntax in guild: '" + guildname + "'");
                                            channel.sendMessage(output.getString("error-wrong-syntax")).queue();
                                        }
                                    }else {
                                        channel.sendMessage(output.getString("error-user-permission")).queue();
                                        System.out.println("no permission in guild: '" + guildname + "'");
                                    }
                                    break;
                                case "enable":
                                    break;
                                case "disable":
                                    
                                    bre
                                default :

                                    break;
                            }

                        }
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
                                    case "clear":
                                        guild.clearModrole();
                                        channel.sendMessage(output.getString("modrole-clear")).queue();
                                        break;
                                    case "auto":
                                        guild.autoModrole(event.getGuild());
                                        channel.sendMessage(output.getString("modrole-auto")).queue();
                                        System.out.println("auto adding moroles to guild: "+guildname);
                                        //break; //break removed to list new modrole list
                                    case "list":
                                        //list all modroles
                                        System.out.println("listing modroles in guild: '" + guildname + "'");
                                        StringBuilder text = new StringBuilder(output.getString("modrole-list") + "\n");
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
                                            if (!memberHasRole(member, mentions.get(0).getIdLong())) {
                                                event.getGuild().getController().addRolesToMember(member, mentions).queue();
                                                channel.sendMessage(output.getString("role-added")).queue();
                                                System.out.println("added a role to '" + member.getEffectiveName() + "'in guild: '" + guildname + "'");
                                            } else {
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
                                            if (memberHasRole(member, mentions.get(0).getIdLong())) {
                                                event.getGuild().getController().removeRolesFromMember(member, mentions).queue();
                                                channel.sendMessage(output.getString("role-removed")).queue();
                                                System.out.println("removed a role to '" + member.getEffectiveName() + "'in guild: '" + guildname + "'");
                                            } else {
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
                                        } else {
                                            System.out.println("wrong syntax in guild : '" + guildname + "'");
                                            channel.sendMessage(output.getString("error-wrong-syntax")).queue();
                                        }
                                    } else {
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
                                    guild.optionRoleGroup(args[1], Arrays.copyOfRange(args, 2, args.length), message, channel);
                                    System.out.println(" in guild: '" + guildname + "'");
                                    break;
                            }
                            break;
                        } else {
                            channel.sendMessage(output.getString("error-user-permission")).queue();
                            System.out.println("no permission in guild: '" + guildname + "'");
                        }

//----------------------------------CUSTOM COMMANDS-------------------------------
                    default: {
                        RoleGroup group = RoleGroup.findGroup(guild.getRoleGroups(), args[0]);
                        if (group != null) {
                            if (memberHasRole(member, group.getBoundRole())) {
                                if (args[1] != null) {
                                    String ret = group.command(event.getGuild(), member, args[1], output);
                                    channel.sendMessage(ret).queue();
                                    System.out.println(" in guild: '" + guildname + "'");
                                }
                            } else {
                                channel.sendMessage(output.getString("error-user-permission")).queue();
                                System.out.println("rolegroup cc - denied in guild: '" + guildname + "'");
                            }
                        }
                    }
                }
            return;
//-------ALL---------------------------EMOJI-DIRECT--------------------------------
            } else {
                String args[] = message.getRawContent().split(System.getenv("DEFAULT_EMOJI_PREFIX"));
                StringBuilder ret = new StringBuilder(args[0]);
                boolean found=false;
                boolean last=false;
                boolean used=false;
                if(args.length>1)
                {
                    for(int i=1;i<args.length;i++)
                    {
                        String arg = args[i];
                        if(!last){
                            if (arg.matches("\\w+\\.\\w+")) {
                                String[] param = arg.split("\\.");
                                String emoji;
                                emoji=emojiGuild.getEmoji(arg,event.getJDA());
                                if(emoji!=null){
                                    ret.append(emoji);
                                    found=true;
                                    used=true;
                                }
                            }
                        }
                        if(!found) {
                            if (!last)
                                ret.append(System.getenv("DEFAULT_EMOJI_PREFIX"));
                            ret.append(arg);
                        }
                        last=found;
                        found=false;
                    }
                }
                if(used){
                    if(PermissionUtil.checkPermission(event.getGuild().getTextChannelById(channel.getId()),event.getGuild().getSelfMember(),Permission.MESSAGE_MANAGE)) {
                        message.delete().queue();
                    }
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setColor(member.getColor());
                    eb.setFooter(member.getEffectiveName(),member.getUser().getAvatarUrl());
                    channel.sendMessage(eb.build()).queue();
                    channel.sendMessage(ret.toString()).queue();
                    return;
                }

            }

//-------ALL---------------------------IGNORED--------------------------------
            //if the message was not directed to the bot
            System.out.println("Ignored");
        }else{
            event.getJDA().shutdown();
            Reconnector.reconnect();
        }
    }

    @Override
    public void onRoleDelete(RoleDeleteEvent event) {
        ResourceBundle output;
        BotGuild guild;
        //name of sender server
        if(checkConnection()) {
            String guildname = event.getGuild().getName();
            //search for existent informations class for server
            guild = findGuild(event.getGuild().getIdLong());
            if (guild == null) {
                //create local instance of server informations
                guild = new BotGuild(event.getGuild(), conn);
                savedGuilds.add(guild);
            }
            //set locales to giuld setting

            output = guild.getMessages();

            if (guild.onRoleDeleted(event.getRole()) || deleted) {
                deleted = false;
                System.out.println("role deleted in guild: " + guildname);
                event.getGuild().getOwner().getUser().openPrivateChannel().queue((channel) ->
                {
                    channel.sendMessage(output.getString("event-role-deleted")).queue();
                    channel.sendMessage(output.getString("event-role-deleted-2")).queue();
                });
            }
        }else{
            event.getJDA().shutdown();
            Reconnector.reconnect();
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        ResourceBundle output;
        BotGuild guild;
        //name of sender server
        String guildname = event.getGuild().getName();
        //search for existent informations class for server
        guild = findGuild(event.getGuild().getIdLong());
        if (guild == null) {
            //create local instance of server informations
            guild = new BotGuild(event.getGuild(), conn);
            savedGuilds.add(guild);
            output = guild.getMessages();
            if(guild.isNew)
            {
                guild.isNew=false;
                System.out.println("guild "+event.getGuild().getName()+" added");
                try {
                    event.getGuild().getDefaultChannel().sendMessage(output.getString("event-join")).queue();
                }catch (InsufficientPermissionException ex)
                {
                    event.getGuild().getOwner().getUser().openPrivateChannel().queue((channel) ->
                    {
                        channel.sendMessage(output.getString("event-join")).queue();
                    });
                }
            }
        }
        updateServerCount(event.getJDA());
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event){
        try{
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM guilds WHERE guildid="+event.getGuild().getIdLong());
            if(rs.next())
            {
                rs.close();
                rs = stmt.executeQuery("SELECT groupid FROM groups WHERE guildid="+event.getGuild().getIdLong());
                while(rs.next())
                {
                    stmt.execute("DELETE FROM grouproles WHERE groupid="+rs.getLong(1));
                }
                rs.close();
                stmt.execute("DELETE FROM groups WHERE guildid="+event.getGuild().getIdLong());
                stmt.execute("DELETE FROM roles WHERE guildid="+event.getGuild().getIdLong());
            }else
                rs.close();
            stmt.execute("DELETE FROM active_emoji_guilds WHERE emoji_guildID="+event.getGuild().getIdLong()+" OR guildid="+event.getGuild().getIdLong());
            stmt.execute("DELETE FROM registered_emoji_server WHERE guildid="+event.getGuild().getIdLong());
            stmt.execute("DELETE FROM guilds WHERE guildid="+event.getGuild().getIdLong());
            stmt.execute("COMMIT");
            stmt.close();
            System.out.println("guild "+event.getGuild().getName()+" has been removed");
        }catch (SQLException ex)
        {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        updateServerCount(event.getJDA());
    }

    private boolean memberHasRole(Member member, Long roleId) {
        List<Role> list = member.getRoles();
        Role role = member.getGuild().getRoleById(roleId);
        return role != null && list.contains(role);
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

                helpMsg.addField("emoji", output.getString("help-def-emoji-mod"), false);
            }else{
                helpMsg.addField("emoji", output.getString("help-def-emoji-user"), false);
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
                case "emoji":
                    helpMsg.setTitle(output.getString("help-emoji-title"));
                    helpMsg.setDescription(output.getString("help-emoji-description"));
                    if(member.isOwner() || guild.memberIsMod(member)) {
                        helpMsg.addField(output.getString("help-field-usage"), output.getString("help-emoji-mod-usage"), false);
                        helpMsg.addField(output.getString("help-field-example"), output.getString("help-emoji-mod-example"), false);
                    }else{
                        helpMsg.addField(output.getString("help-field-usage"), output.getString("help-emoji-user-usage"), false);
                        helpMsg.addField(output.getString("help-field-example"), output.getString("help-emoji-user-example"), false);
                    }
                    helpMsg.addField(output.getString("help-field-dyk"), output.getString("help-emoji-dyk"), false);
                    break;
                default :
                    helpMsg.setTitle(output.getString("help-404-title"));
                    helpMsg.setDescription(output.getString("help-404-cmd"));
            }
        channel.sendMessage(helpMsg.build()).queue();
    }

    private boolean checkConnection()
    {
        try {
            Statement stmt= conn.createStatement();
            stmt.execute("SELECT 1");
            stmt.close();
            return true;
        }catch (SQLException ex)
        {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return false;
    }

    public MyListener(Connection conn, List<BotGuild> savedGuilds) {
        this.conn = conn;
        this.savedGuilds = savedGuilds;
        this.emojiGuild=new EmojiGuild(conn);
    }

    private static void guildDeleteDB(Connection conn,Long guildId)
    {
        try{
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM guilds WHERE guildid="+guildId);
            List<Long> to_remove = new ArrayList<>();
            if(rs.next())
            {
                rs.close();
                rs = stmt.executeQuery("SELECT groupid FROM groups WHERE guildid="+guildId);
                while(rs.next())
                {
                    to_remove.add(rs.getLong(1));
                }
                rs.close();
                for (Long id : to_remove)
                {
                    stmt.execute("DELETE FROM grouproles WHERE groupid="+id);
                }
                stmt.execute("DELETE FROM groups WHERE guildid="+guildId);
                stmt.execute("DELETE FROM roles WHERE guildid="+guildId);
            }else {
                rs.close();
            }
            stmt.execute("DELETE FROM guilds WHERE guildid="+guildId);
            stmt.execute("COMMIT");
            stmt.close();
        }catch (SQLException ex)
        {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }

    private void updateServerCount(JDA api)
    {
        String url = "https://discordbots.org/api/bots/"+api.getSelfUser().getId()+"/stats";
        String discordbots_key = System.getenv("DISCORDBOTS_KEY");

        JSONObject data = new JSONObject();
        data.put("server_count", api.getGuilds().size());

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), data.toString());

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("User-Agent", "DiscordBot " + api.getSelfUser().getName())
                .addHeader("Authorization", discordbots_key)
                .build();

        try {
            new OkHttpClient().newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
