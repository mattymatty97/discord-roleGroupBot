package com.testBot;

import net.dv8tion.jda.core.JDA;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class EmojiGuild {
    private boolean isOpen;         /**bool value to test validity of object**/
    protected Connection conn;        /**SQL connection object send by main**/
    protected long guildId;                /**guild id used to identify guild**/
    protected String prefix;          /**prefix for trig a reaction**/
    protected List<RegisteredEmojiGuild> activeGuilds;
    protected int maxguilds;

    public List<RegisteredEmojiGuild> getActiveGuilds() {
        return activeGuilds;
    }

    public long getGuildId() {
        return guildId;
    }

    public String getPrefix() {
        return prefix;
    }

    public String printEmojiList(JDA api)
    {
        StringBuilder ret = new StringBuilder();

        for(RegisteredEmojiGuild guild : activeGuilds){
            ret.append(guild.getEmojiList(api));
        }
        return ret.toString();
    }

    public String addGuild(List<EmojiGuild> guilds,String title,ResourceBundle output)
    {
        Statement stmt;
        StringBuilder ret = new StringBuilder();
        boolean found= false;
        if(activeGuilds.size()<maxguilds) {
            for (EmojiGuild guild : guilds) {
                if (guild instanceof RegisteredEmojiGuild) {
                    RegisteredEmojiGuild rguild = (RegisteredEmojiGuild) guild;
                    if (rguild.getTitle().equals(title)){
                        try
                        {
                            stmt = conn.createStatement();
                            stmt.execute("INSERT INTO \"active-emoji-guilds\"(guildid,\"emoji-guildID\") VALUES ("+guildId+","+rguild.getGuildId()+")");
                            activeGuilds.add(rguild);
                            found=true;
                            break;
                        }catch(SQLException ex)
                        {
                            System.out.println("SQLException: " + ex.getMessage());
                            System.out.println("SQLState: " + ex.getSQLState());
                            System.out.println("VendorError: " + ex.getErrorCode());
                            break;
                        }
                    }
                }
            }
            if(found){
                ret.append(output.getString("emoji-add"));
            }else{
                ret.append(output.getString("error-emoji-set-404"));
            }
        }else{
            ret.append(output.getString("error-emoji-limit"));
        }
        return ret.toString();
    }

    public String removeGuild(List<EmojiGuild> guilds,String title,ResourceBundle output)
    {
        Statement stmt;
        StringBuilder ret = new StringBuilder();
        boolean found= false;
            for (RegisteredEmojiGuild guild : activeGuilds) {
                    if (guild.getTitle().equals(title)){
                        try
                        {
                            stmt = conn.createStatement();
                            stmt.execute("DELETE FROM \"active-emoji-guilds\" WHERE guildid="+guildId+" AND 'emoji-guildID'="+guild.getGuildId());
                            activeGuilds.remove(guild);
                            found=true;
                            break;
                        }catch(SQLException ex)
                        {
                            System.out.println("SQLException: " + ex.getMessage());
                            System.out.println("SQLState: " + ex.getSQLState());
                            System.out.println("VendorError: " + ex.getErrorCode());
                            break;
                        }
                }
            if(found){
                ret.append(output.getString("emoji-remove"));
            }else{
                ret.append(output.getString("error-emoji-set-404"));
            }
        }
        return ret.toString();
    }

    public String registerGuild(List<EmojiGuild> guilds,String title,ResourceBundle output){
        StringBuilder ret=new StringBuilder();
        Statement stmt;
        ResultSet rs;
        try
        {
            stmt=conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM \"registered-emoji-server\" WHERE title='"+title+"'");
            if(rs.next())
            {
                ret.append(output.getString("error-emoji-title-used"));
            }else {
                stmt.execute("INSERT INTO \"registered-emoji-server\"(guildid, title) VALUES (" + guildId + ",'"+title+"')");
                promoteGuild(guilds,title);
                ret.append(output.getString("emoji-guild-registered"));
            }
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return ret.toString();
    }

    public void promoteGuild(List<EmojiGuild> guilds,String title)
    {
        RegisteredEmojiGuild promoted =
                new RegisteredEmojiGuild(conn,guildId,prefix,activeGuilds,maxguilds,title);
        guilds.remove(this);
        guilds.add(promoted);
    }

    public void readActive(List<EmojiGuild> guilds)
    {
        Statement stmt;
        ResultSet rs;
        try
        {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT \"emoji-guildID\" FROM \"active-emoji-guilds\" WHERE guildid="+guildId);
            while (rs.next()){
                long emojiguild =rs.getLong(1);
                for (EmojiGuild guild : guilds){
                    if(guild instanceof RegisteredEmojiGuild) {
                        RegisteredEmojiGuild rguild = (RegisteredEmojiGuild) guild;
                        if (guild.getGuildId() == emojiguild) {
                            this.activeGuilds.add(rguild);
                            break;
                        }
                    }
                }
            }
            rs.close();
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }

    EmojiGuild(long guild, Connection actconn)
    {
        this.conn = actconn;
        this.activeGuilds = new ArrayList<>();
        this.guildId = guild;
        this.maxguilds=4;

        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT guildid,\"emoji-prefix\",\"max-emoji\" FROM guilds WHERE guildid=" + guildId);

            if (rs.next()) {
                this.prefix=rs.getString(2);
                this.maxguilds=rs.getInt(3);
            }
            rs.close();
            stmt.close();
            this.isOpen = true;
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }
    EmojiGuild(Connection conn, long guildId, String prefix,List<RegisteredEmojiGuild> activeguilds,int maxguilds){
        this.conn=conn;
        this.guildId=guildId;
        this.prefix=prefix;
        this.activeGuilds=activeguilds;
        this.maxguilds=maxguilds;
        this.isOpen=true;
    }
    EmojiGuild(Connection conn, long guildId, String prefix,int maxguilds)
    {
        this.conn=conn;
        this.guildId=guildId;
        this.prefix=prefix;
        this.activeGuilds=new ArrayList<>();
        this.maxguilds=maxguilds;
        this.isOpen=true;
    }

    public static String printRegistered(List<EmojiGuild> guilds,JDA api){
        StringBuilder ret = new StringBuilder();
            for (EmojiGuild guild : guilds)
            {
                if(guild instanceof RegisteredEmojiGuild) {
                    RegisteredEmojiGuild rguild = (RegisteredEmojiGuild) guild;
                    ret.append("\n");
                    ret.append(rguild.getTitle());
                    ret.append("  ");
                    ret.append(api.getGuildById(rguild.getGuildId()).getName());
                }
            }
        return ret.toString();
    }


}