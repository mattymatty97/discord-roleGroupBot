import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.sql.*;

import java.util.Comparator;
import java.util.List;

public class MyListener extends ListenerAdapter {
    private Connection conn;
    private List<BotGuild> savedGuilds;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        BotGuild guild;
        guild = findGuild(event.getGuild().getIdLong());
        if (guild == null) {
            guild = new BotGuild(event.getGuild().getIdLong(), event.getGuild().getName().intern(), conn);
            savedGuilds.add(guild);
        }
        if (event.getAuthor().isBot()) return;
        Member member = event.getMember();
        MessageChannel channel = event.getChannel();
        Message message = event.getMessage();
        String content = message.getContent();
        if (content.substring(0, guild.getPrefix().length()).equals(guild.getPrefix())) {
            String[] args = content.substring(guild.getPrefix().length()).split(" ");
            switch (args[0]) {
                case "help":
                    channel.sendMessage("help of testbot\n" +
                            "- ping: answers Pong!\n" +
                            "mod commands:\n" +
                            "- set prefix [prefix]: changes bot prefix for this server\n" +
                            "- modrole <add/remove/list> [roleMention]").queue();
                    break;
                case "ping":
                case "Ping":
                    channel.sendMessage("Pong!").queue(); // Important to call .queue() on the RestAction returned by sendMessage(...)
                    break;
                case "set":
                    if (member.isOwner() || guild.memberIsMod(member)) {
                        switch (args[1]) {
                            case "prefix":
                                if (args[2] != null) {
                                    if (args[2].length() > 10) {
                                        channel.sendMessage("Error too long prefix (limit is 10)!").queue();
                                        break;
                                    }
                                    guild.setPrefix(args[2]);
                                    channel.sendMessage("Prefix set!").queue();
                                }
                                break;
                        }
                    } else {
                        channel.sendMessage("Error you have not permission to do this!").queue();
                    }
                    break;
                case "modrole":
                    if (member.isOwner() || guild.memberIsMod(member)) {
                        if (args[1] != null) {
                            List<Role> mentions = message.getMentionedRoles();
                            switch (args[1]) {
                                case "add":
                                    if (mentions.size() == 1) {
                                        guild.addModRole(mentions.get(0).getIdLong(), mentions.get(0).getName());
                                        channel.sendMessage("Role added!").queue();
                                    } else
                                        channel.sendMessage("wrong syntax!").queue();
                                    break;
                                case "remove":
                                    if (mentions.size() == 1) {
                                        guild.removeModRole(mentions.get(0).getIdLong());
                                        channel.sendMessage("Role removed!").queue();
                                    } else
                                        channel.sendMessage("wrong syntax!").queue();
                                    break;
                                case "list":
                                    String text = "Active ModRoles:\n";
                                    for (Long id : guild.getModRolesById()) {
                                        for (Role role : event.getGuild().getRoles()) {
                                            if (role.getIdLong() == (id))
                                                text += role.getName() + "\n";
                                        }
                                    }
                                    channel.sendMessage(text).queue();
                                    break;
                                default:
                                    channel.sendMessage("wrong syntax!").queue();
                            }

                        }
                        break;
                    } else {
                        channel.sendMessage("Error you have not permission to do this!").queue();
                    }
                    break;
                case "role":
                    if (member.isOwner() || guild.memberIsMod(member)) {
                        if (args[1] != null) {
                            List<Role> mentions = message.getMentionedRoles();
                            switch (args[1]) {
                                case "add":
                                    if (mentions.size() == 1) {
                                        List<Role> roles = event.getGuild().getSelfMember().getRoles();
                                        if(roles.get(0).getPosition() > mentions.get(0).getPosition()) {
                                            event.getGuild().getController().addRolesToMember(member, mentions).queue();
                                            channel.sendMessage("Role added!").queue();
                                        }else{
                                            channel.sendMessage("Cannot modify a higher or equal role to my higher role!").queue();
                                        }
                                    } else
                                        channel.sendMessage("wrong syntax!").queue();
                                    break;
                                case "remove":
                                    if (mentions.size() == 1) {
                                        List<Role> roles = event.getGuild().getSelfMember().getRoles();
                                        if(roles.get(0).getPosition() > mentions.get(0).getPosition()) {
                                            event.getGuild().getController().removeRolesFromMember(member, mentions).queue();
                                            channel.sendMessage("Role removed!").queue();
                                        }else{
                                            channel.sendMessage("Cannot modify a higher or equal role to my higher role!").queue();
                                        }
                                    } else
                                        channel.sendMessage("wrong syntax!").queue();
                                    break;
                                case "list":
                                    String text = "Active ModRoles:\n";
                                    Guild g = event.getGuild();
                                    for (Long id : guild.getModRolesById()) {
                                        text +=  g.getRoleById(id).getName() + "\n";
                                    }
                                    channel.sendMessage(text).queue();
                                    break;
                                default:
                                    channel.sendMessage("wrong syntax!").queue();
                            }

                        }
                        break;
                    } else {
                        channel.sendMessage("Error you have not permission to do this!").queue();
                    }
                    break;
            }
        }
    }

    private BotGuild findGuild(Long guildId) {
        for (BotGuild guild : savedGuilds) {
            if (guild.getId().equals(guildId))
                return guild;
        }
        return null;
    }


    public MyListener(Connection conn, List<BotGuild> savedGuilds) {
        this.conn = conn;
        this.savedGuilds = savedGuilds;
    }
}