package com.testBot;

import java.sql.*;
import java.util.List;
import java.util.ResourceBundle;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;

public class RegisteredEmojiGuild extends EmojiGuild {
    private String title;

    public String getTitle() {
        return title;
    }

    public void demoteGuild(List<EmojiGuild> guilds)
    {
        EmojiGuild demoted = new EmojiGuild(conn,guildId,prefix,activeGuilds,maxguilds);
        guilds.remove(this);
        guilds.add(demoted);
    }

    public String unRegisterGuild(List<EmojiGuild> guilds,ResourceBundle output){
        StringBuilder ret=new StringBuilder();
        Statement stmt;
        try
        {
            stmt=conn.createStatement();
            stmt.execute("DELETE FROM \"registered-emoji-server\" WHERE guildid="+guildId);
            demoteGuild(guilds);
            ret.append(output.getString("emoji-guild-unregistered"));
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return ret.toString();
    }

    public String getEmoji(String name, JDA api){
        Guild guild = api.getGuildById(guildId);
        if(guild!=null)
        {
            List<Emote> emote = guild.getEmotesByName(name,false);
            if(emote.size()==1)
            {
                return emote.get(0).getAsMention();
            }
        }
        return null;
    }

    public String getEmojiList(JDA api)
    {
        Guild guild = api.getGuildById(guildId);
        StringBuilder ret = new StringBuilder();
        if(guild!=null) {
            ret.append(guild.getName()).append(":");
            List<Emote> emotes = guild.getEmotes();
            for (Emote emote : emotes){
                ret.append("\n").append(title);
                ret.append(".").append(emote.getName()).append("  ");
                ret.append(emote.getAsMention());
            }
        }
        return ret.toString();
    }

    RegisteredEmojiGuild(Connection conn, long guildId, String prefix,List<RegisteredEmojiGuild> activeguilds,int maxguilds,String title){
        super(conn,guildId,prefix,activeguilds,maxguilds);
        this.title=title;
    }

    RegisteredEmojiGuild(long guild, Connection actconn){
        super(guild,actconn);
        try{
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT title FROM \"registered-emoji-server\" WHERE guildid="+guild);
            if(rs.next())
            {
                this.title = rs.getString(1);
            }
            rs.close();
            stmt.close();
        }catch(SQLException ex)
        {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }
}
