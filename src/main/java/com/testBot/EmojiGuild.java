package com.testBot;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Guild;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class EmojiGuild {
    private Connection conn;        /**SQL connection object send by main**/

    public String registerGuild(long guildId,String title,ResourceBundle output){
        StringBuilder ret=new StringBuilder();
        Statement stmt;
        ResultSet rs;
        try
        {
            stmt=conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM registered_emoji_server WHERE guildid="+guildId);
            if(rs.next()){
                ret.append(output.getString("error-emoji-registered"));
                System.out.print("emoji not registered");
                rs.close();
            }else {
                rs.close();
                rs = stmt.executeQuery("SELECT * FROM registered_emoji_server WHERE title='" + title + "'");
                if (rs.next()) {
                    ret.append(output.getString("error-emoji-title-used"));
                    System.out.print("emoji not registered");
                } else {
                    stmt.execute("INSERT INTO registered_emoji_server(guildid, title) VALUES (" + guildId + ",'" + title + "')");
                    ret.append(output.getString("emoji-guild-registered"));
                    System.out.print("emoji registered");
                }
            }
            stmt.close();
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return ret.toString();
    }

    public String unRegisterGuild(long guildId,ResourceBundle output){
        StringBuilder ret=new StringBuilder();
        Statement stmt;
        ResultSet rs;
        try
        {
            stmt=conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM registered_emoji_server WHERE guildid="+guildId);
            if(rs.next()) {
                rs.close();
                stmt.execute("DELETE FROM active_emoji_guilds WHERE emoji_guildID=" + guildId);
                stmt.execute("DELETE FROM registered_emoji_server WHERE guildid=" + guildId);
                System.out.print("emoji unregistered");
                ret.append(output.getString("emoji-guild-unregistered"));
            }else{
                System.out.print("emoji not unregistered");
                ret.append(output.getString("error-emoji-unregistered"));
            }
            stmt.close();
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return ret.toString();
    }

    public String getEmoji(String arg,JDA api){
        String ret = null;
        Statement stmt;
        ResultSet rs;
        String args[] = arg.split("\\.");
        try
        {
            stmt=conn.createStatement();
            rs = stmt.executeQuery("SELECT guildid FROM registered_emoji_server WHERE title='"+args[0]+"'");
            if(rs.next()) {
                Guild guild = api.getGuildById(rs.getLong(1));
                List<Emote> emoji = guild.getEmotesByName(args[1],false);
                if(emoji.size()==1)
                {
                    ret = emoji.get(0).getAsMention();
                }
            }
            rs.close();
            stmt.close();
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return ret;
    }

    public String getEmojiList(String title,JDA api){
        StringBuilder ret = new StringBuilder();
        Statement stmt;
        ResultSet rs;
        try
        {
            stmt=conn.createStatement();
            rs = stmt.executeQuery("SELECT guildid FROM registered_emoji_server WHERE title='"+title+"'");
            if(rs.next()) {
                Guild guild = api.getGuildById(rs.getLong(1));
                ret.append(guild.getName());
                List<Emote> emoji = guild.getEmotes();
                for (Emote emote : emoji){
                    ret.append("\n");
                    ret.append(title).append(".");
                    ret.append(emote.getName());
                    ret.append("   ").append(emote.getAsMention());
                }
            }
            rs.close();
            stmt.close();
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return ret.toString();
    }

    public String printServers(JDA api){
        StringBuilder ret = new StringBuilder();
        Statement stmt;
        ResultSet rs;
        try{
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT guildid,title FROM registered_emoji_server");
            while (rs.next()){
                ret.append("\n");
                ret.append(rs.getString(2));
                ret.append("   ");
                ret.append(api.getGuildById(rs.getLong(1)).getName());
            }
            stmt.close();
        }catch (SQLException ex)
        {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return ret.toString();
    }

    EmojiGuild(Connection actconn)
    {
        this.conn = actconn;
    }
}