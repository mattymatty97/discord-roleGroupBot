package com.roleGroup;

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
    private static List<String> reservedNames = Arrays.asList("ping", "help", "modrole", "role", "create", "delete", "list");
    private Connection conn;
    public static boolean deleted = false;

    private static String prefix = System.getenv("BOT_PREFIX");

    @Override
    public void onReady(ReadyEvent event) {
        Statement stmt;
        if(!prefix.endsWith("b")) {
            try {
                stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT guildid FROM guilds");
                List<Long> to_remove = new ArrayList<>();
                while (rs.next()) {
                    boolean found = false;
                    for (Guild guild : event.getJDA().getSelfUser().getMutualGuilds()) {
                        if (guild.getIdLong() == rs.getLong(1)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        to_remove.add(rs.getLong(1));
                }
                rs.close();
                stmt.close();
                for (Long guildId : to_remove)
                    guildDeleteDB(conn, guildId);
            } catch (SQLException ex) {
                System.out.println("SQLException: " + ex.getMessage());
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("VendorError: " + ex.getErrorCode());
            }
            updateServerCount(event.getJDA());
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        //locales generation (dynamic strings from file selectionable by language)
        ResourceBundle output;
        if (checkConnection()) {
            //if is a direct message exit immediately
            if (event.isFromType(ChannelType.PRIVATE)) return;
            //if is a bot exit immediately
            if (event.getAuthor().isBot()) return;
            //if i cant write
            if (!PermissionUtil.checkPermission(event.getTextChannel(), event.getGuild().getSelfMember(), Permission.MESSAGE_WRITE))
                return;

            BotGuild guild;
            //name of sender server
            String guildname = event.getGuild().getName();
            //search for existent informations class for server
            guild = new BotGuild(event.getGuild(), conn);

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
            //get sender member
            Member member = event.getMember();
            //get channel to send

            MessageChannel channel = event.getChannel();
            //get message
            Message message = event.getMessage();
            //get text
            String content = message.getContentRaw().toLowerCase();

            //if length is enough test if the message starts with right prefix
            if (content.length() > prefix.length() && content.substring(0, prefix.length()).equals(prefix)) {
                //split by spaces into args
                String[] args = content.substring(prefix.length()).split(" +");
                //test first argument
                System.out.println("Message from '" + member.getEffectiveName() + "' in guild '" + guildname + "'");
                switch (args[0].toCharArray()[0]) {
                    case '!':
                        switch (args[0].substring(1)) {

//------USER---------------------HELP--------------------------------------

                            case "help":
                                System.out.println("help shown in guild: '" + guildname + "'");
                                PrintHelp(channel, member, guild, event.getGuild(), args);
                                break;

//------USER--------------------PING---------------------------------------

                            case "ping":
                            case "Ping":
                                System.out.println("Ping executed in guild: '" + guildname + "'");
                                channel.sendMessage(output.getString("pong")).queue(); // Important to call .queue() on the RestAction returned by sendMessage(...)
                                break;
//------USER-------------------SUPPORT---------------------------------------
                            case "support":
                                channel.sendMessage(output.getString("support-msg")).queue();
                                break;
//------USER-------------------OTHER---------------------------------------
                            case "other":
                                channel.sendMessage(output.getString("other-msg")).queue();
                                break;
//----------------------------------CUSTOM COMMANDS-------------------------------
                            default: {
                                RoleGroup group = RoleGroup.getRolegroup(event.getGuild(), conn, args[0].substring(1).toLowerCase());
                                String argument;
                                if(args.length == 2)
                                    argument=args[1];
                                else
                                    argument=null;
                                if (group != null) {
                                    if (group.memberAllowed(member)) {
                                        String ret = group.command(event.getGuild(), member, argument, output);
                                        channel.sendMessage(ret).queue();
                                        System.out.println(" in guild: '" + guildname + "'");
                                    } else {
                                        channel.sendMessage(output.getString("error-user-permission")).queue();
                                        System.out.println("rolegroup cc - denied in guild: '" + guildname + "'");
                                    }
                                }
                            }
                        }
                        break;

                    case '@':
                        if (memberIsOwner(member) || member.isOwner() || guild.memberIsMod(member)) {
                            switch (args[0].substring(1)) {
//-------MOD-------------------MODROLE-------------------------------------

                                case "modrole":
                                    //if there are other arguments
                                    if (args.length>1 && !args[1].isEmpty()) {
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
                                                System.out.println("auto adding moroles to guild: " + guildname);
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


//-------MOD-----------------------ROLE------------------------------------------------

                                case "role":
                                    //get mentions list
                                    if(args.length>1) {
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
                                    }
                                    break;

//-------MOD--------------------------ROLEGROUP-------------------------------

                                case "create": {
                                    if (args.length > 1 && !args[1].isEmpty()) {
                                            //if the argument is not a mentioned role
                                            if (args[1].matches("\\w+")) {
                                                //call the class method
                                                if (nameIsAllowed(args[1].toLowerCase())) {
                                                    if (RoleGroup.createRolegroup(event.getGuild(),args[1].toLowerCase(), conn) != null) {
                                                        System.out.println("created rolegroup '" + args[1] + "' in guild: '" + guildname + "'");
                                                        channel.sendMessage(output.getString("rolegroup-created")).queue();
                                                    } else {
                                                        System.out.println("found existent rolegroup in guild: '" + guildname + "'");
                                                        channel.sendMessage(output.getString("error-existing-rolegroup")).queue();
                                                    }
                                                } else {
                                                    System.out.println("name reserverd in guild: '" + guildname + "'");
                                                    channel.sendMessage(output.getString("error-name-reserved")).queue();
                                                }
                                            } else {
                                                System.out.println("wrong syntax in guild : '" + guildname + "'");
                                                channel.sendMessage(output.getString("error-wrong-syntax")).queue();
                                            }
                                        } else {
                                            System.out.println("wrong syntax in guild: '" + guildname + "'");
                                            channel.sendMessage(output.getString("error-role-mention")).queue();
                                        }
                                }
                                break;

                                case "delete":
                                    //if there is an arg
                                    if (args.length == 2) {
                                        //call the class method
                                        RoleGroup group = RoleGroup.getRolegroup(event.getGuild(), conn, args[1]);
                                        if (group != null) {
                                            group.delete();
                                            System.out.println("deleted rolegroup '" + args[1] + "' in guild: '" + guildname + "'");
                                            channel.sendMessage(output.getString("rolegroup-deleted")).queue();
                                        } else {
                                            System.out.println("found unexistent rolegroup in guild: '" + guildname + "'");
                                            channel.sendMessage(output.getString("error-rolegroup-not-exist")).queue();
                                        }
                                    }
                                    break;

                                case "list": {
                                    StringBuilder str = new StringBuilder("```diff\n").append(output.getString("rolegroup-listing")).append("\n");
                                    for (String rolegroup : RoleGroup.listRoleGroups(event.getGuild(), conn)) {
                                        str.append(rolegroup).append("\n");
                                    }
                                    str.append("```");
                                    channel.sendMessage(str.toString()).queue();
                                    System.out.println("listing rolegroups in guild: '" + guildname + "'");
                                }
                                break;

//----------MOD-----------------RoleGroupModify-----------------------------------------------------------------
                                default:
                                    RoleGroup group = RoleGroup.getRolegroup(event.getGuild(), conn, args[0].substring(1).toLowerCase());
                                    if (group != null) {
                                        if (args.length > 1) {
                                            switch (args[1]) {
                                                case "add": {
                                                    List<Role> list = message.getMentionedRoles();
                                                    //if there is a mention and the syintax is correct
                                                    if (list.size() == 1 && args.length == 5 && args[3].equals("as")) {
                                                        String ret = group.addRole(list.get(0), args[4].toLowerCase(), output);
                                                        channel.sendMessage(ret).queue();
                                                    } else {
                                                        System.out.print("grouproles - wrong syntax");
                                                        channel.sendMessage(output.getString("error-wrong-syntax")).queue();
                                                    }
                                                }
                                                break;

                                                case "remove":{
                                                    if (args[2]!=null){
                                                        String ret = group.removeRole(args[2], output);
                                                        channel.sendMessage(ret).queue();
                                                    } else {
                                                        System.out.print("grouproles - wrong syntax");
                                                        channel.sendMessage(output.getString("error-wrong-syntax")).queue();
                                                    }
                                                }
                                                break;

                                                case "reset":{
                                                    String ret = group.resetRole(output);
                                                    channel.sendMessage(ret).queue();
                                                }
                                                break;

                                                case "type":{
                                                    if (args[2] != null) {
                                                        try {
                                                            RoleGroup.Type type = RoleGroup.Type.valueOf(args[2].toUpperCase());

                                                            String ret = group.setType(type,output);
                                                            channel.sendMessage(ret).queue();
                                                        }catch (IllegalArgumentException ex){
                                                            System.out.print("grouproles - type not found");
                                                            channel.sendMessage(output.getString("error-rolegroup-404-type")).queue();
                                                        }
                                                    } else {
                                                        System.out.print("grouproles - wrong syntax");
                                                        channel.sendMessage(output.getString("error-wrong-syntax")).queue();
                                                    }
                                                }
                                                break;

                                                case "expression":{
                                                    List<Role> list = message.getMentionedRoles();
                                                    //if there is a mentioned role
                                                    if (list.size() > 0) {
                                                        Message msg = channel.sendMessage(output.getString("rolegroup-expression-evaluating")).complete();
                                                        Thread.yield();
                                                        StringBuilder sb = new StringBuilder(args[2]);
                                                        for (int i=3;i<args.length;i++)
                                                            sb.append(" ").append(args[i]);
                                                        String ret = group.setTriggerExpr(sb.toString(),output);
                                                        msg.editMessage(ret).queue();
                                                    }else {
                                                        System.out.print("wrong syntax ");
                                                        channel.sendMessage(output.getString("error-wrong-syntax")).queue();
                                                    }
                                                }
                                                break;

                                                case "status":{
                                                    channel.sendMessage(group.printStatus(output)).queue();
                                                }
                                                break;

                                                case "enable":{
                                                    channel.sendMessage(group.enable(output)).queue();
                                                }
                                                break;

                                                case "disable":{
                                                    channel.sendMessage(group.disable(output)).queue();
                                                }
                                                break;

                                                default:
                                                    break;
                                            }
                                        }
                                    }
                            }
                        } else {
                            channel.sendMessage(output.getString("error-user-permission")).queue();
                            System.out.println("no permission in guild: '" + guildname + "'");
                        }
                }
            }
        } else

        {
            event.getJDA().shutdown();
            Reconnector.reconnect();
        }

    }

    @Override
    public void onRoleDelete(RoleDeleteEvent event) {
        ResourceBundle output;
        BotGuild guild;
        //name of sender server
        if (checkConnection()) {
            String guildname = event.getGuild().getName();
            //search for existent informations class for server
            guild = new BotGuild(event.getGuild(), conn);
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
        } else {
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
            //create local instance of server informations
            guild = new BotGuild(event.getGuild(), conn);
            output = guild.getMessages();
            if (guild.isNew) {
                guild.isNew = false;
                System.out.println("guild " + event.getGuild().getName() + " added");
                try {
                    Optional.ofNullable(event.getGuild().getDefaultChannel()).orElse(event.getGuild().getSystemChannel()).sendMessage(output.getString("event-join")).queue();
                } catch (InsufficientPermissionException ex) {
                    event.getGuild().getOwner().getUser().openPrivateChannel().queue((channel) ->
                    {
                        channel.sendMessage(output.getString("event-join")).queue();
                    });
                }
            }
        updateServerCount(event.getJDA());
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM guilds WHERE guildid=" + event.getGuild().getIdLong());
            if (rs.next()) {
                rs.close();
                rs = stmt.executeQuery("SELECT groupid FROM groups WHERE guildid=" + event.getGuild().getIdLong());
                while (rs.next()) {
                    stmt.execute("DELETE FROM grouproles WHERE groupid=" + rs.getLong(1));
                }
                rs.close();
                stmt.execute("DELETE FROM groups WHERE guildid=" + event.getGuild().getIdLong());
                stmt.execute("DELETE FROM roles WHERE guildid=" + event.getGuild().getIdLong());
            } else
                rs.close();
            stmt.execute("COMMIT");
            stmt.close();
            System.out.println("guild " + event.getGuild().getName() + " has been removed");
        } catch (SQLException ex) {
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

    private boolean nameIsAllowed(String name){
        return !reservedNames.contains(name);
    }

    //prints the help message
    private void PrintHelp(MessageChannel channel, Member member, BotGuild guild,Guild jdaGuild, String[] args) {
        ResourceBundle output = guild.getMessages();
        EmbedBuilder helpMsg = new EmbedBuilder();
        StringBuilder str = new StringBuilder();
        helpMsg.setColor(Color.GREEN);

        if (args.length == 1) {
            //help is dynamic (different for every user)
            helpMsg.setTitle(output.getString("help-title"));
            helpMsg.addField("rg!help", output.getString("help-def-help"), false);
            helpMsg.addField("rg!ping", output.getString("help-def-ping"), false);

            helpMsg.addField("rg!support", output.getString("help-def-support"), false);
            helpMsg.addField("rg!other", output.getString("help-def-other"), false);

            //if is allowed to use mod commands
            if (memberIsOwner(member) || member.isOwner() || guild.memberIsMod(member)) {
                helpMsg.addBlankField(false);
                helpMsg.addField("MOD commands:", "", false);

                helpMsg.addField("rg@modrole", output.getString("help-def-modrole"), false);

                helpMsg.addField("rg@role", output.getString("help-def-role"), false);

                helpMsg.addField("rolegroup", output.getString("help-def-rolegroup"), false);

            }
            helpMsg.addBlankField(false);

            RoleGroup.listRoleGroups(jdaGuild,conn).stream().map((String s)->RoleGroup.getRolegroup(jdaGuild,conn,s))
                    .filter(Objects::nonNull).filter(RoleGroup::isEnabled).filter(rg -> rg.memberAllowed(member))
                    .forEach(rg -> {
                        str.append("\nrg!");
                        str.append(rg.getName());
                        str.append("\n");
                    });

            helpMsg.addField("CUSTOM COMMANDS:", str.toString(), false);

            helpMsg.addField("", output.getString("help-last"), false);
            helpMsg.setFooter(output.getString("help-footer"), null);
        } else
            switch (args[1]) {
                case "ping":
                    helpMsg.setTitle(output.getString("help-ping-title"));
                    helpMsg.addField(output.getString("help-ping"), "", false);
                    break;
                case "help":
                    helpMsg.setTitle(output.getString("help-help-title"));
                    helpMsg.addField(output.getString("help-help"), "", false);
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
                default:
                    helpMsg.setTitle(output.getString("help-404-title"));
                    helpMsg.setDescription(output.getString("help-404-cmd"));
            }
        channel.sendMessage(helpMsg.build()).queue();
    }

    private boolean memberIsOwner(Member member){
        String owner_id = System.getenv("OWNER_ID");
        if ( owner_id == null || owner_id.isEmpty())
            return false;

        Long id = Long.parseLong(owner_id);
        return member.getUser().getIdLong() == id;
    }

    private boolean checkConnection() {
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("SELECT 1");
            stmt.close();
            return true;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return false;
    }

    public MyListener(Connection conn) {
        this.conn = conn;
    }

    private static void guildDeleteDB(Connection conn, Long guildId) {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM guilds WHERE guildid=" + guildId);
            List<Long> to_remove = new ArrayList<>();
            if (rs.next()) {
                rs.close();
                rs = stmt.executeQuery("SELECT groupid FROM groups WHERE guildid=" + guildId);
                while (rs.next()) {
                    to_remove.add(rs.getLong(1));
                }
                rs.close();
                for (Long id : to_remove) {
                    stmt.execute("DELETE FROM grouproles WHERE groupid=" + id);
                }
                stmt.execute("DELETE FROM groups WHERE guildid=" + guildId);
                stmt.execute("DELETE FROM roles WHERE guildid=" + guildId);
            } else {
                rs.close();
            }
            stmt.execute("DELETE FROM guilds WHERE guildid=" + guildId);
            stmt.execute("COMMIT");
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }

    private void updateServerCount(JDA api) {
        String url = "https://discordbots.org/api/bots/" + api.getSelfUser().getId() + "/stats";
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
