package com.testBot;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;

/**
 * @author mattymatty
 * local class for storing per guild informations.
 */
public class BotGuild {
    private boolean isOpen;         /**bool value to test validity of object**/
    private Connection conn;        /**SQL connection object send by main**/
    private Long guildId;                /**guild id used to identify guild**/
    private String prefix;          /**prefix for trig a reaction**/
    private List<Long> modRolesById;/**list of roles (stored by id) that are allowed to run mod commands**/
    private List<RoleGroup> roleGroups;
    private EmojiGuild emojiGuild;
    private Locale locale;
    public  boolean isNew = true;


    public ResourceBundle getMessages()
    {
        return ResourceBundle.getBundle("messages",locale);
    }

    /**
     * getter to id attribute
     * @return the guild id in format Long
     */
    public Long getId()
    {
        if(!isOpen)
            return null;
        return guildId;
    }

    /**
     * getter to prefix attribute
     * @return guild prefix in String format
     */
    public String getPrefix()
    {
        if(!isOpen)
            return null;
        return prefix;
    }

    /**
     * getter to modroles attribute
     * @return modroles in format List of Roles
     */
    public List<Long> getModRolesById() {
        if(!isOpen)
            return null;
        return modRolesById;
    }

    public List<RoleGroup> getRoleGroups() {
        if(!isOpen)
            return null;
        return roleGroups;
    }

    public EmojiGuild getEmojiGuild() {
        return emojiGuild;
    }

    /**
     * setter to the prefix attribute
     * it also updates it on the remote db
     * @param n_prefix new prefix to be set
     * @return self object, null if on error
     */
    public BotGuild setPrefix(String n_prefix)
    {
        Statement stmt;
        try {
            stmt = conn.createStatement();
            stmt.execute("UPDATE guilds SET prefix='"+ n_prefix +"' WHERE guildId="+this.guildId);
            stmt.execute("COMMIT");
            this.prefix = n_prefix;
            stmt.close();
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            return null;
        }
        return this;
    }

    /**
     * test if a member is in the modrole list
     * @param member to test
     * @return true if is authorized false otherwise
     */
    public boolean memberIsMod(Member member)
    {
        List<Role> roles = member.getRoles();

        for (Long role: modRolesById)
        {
            Integer i,n;
            n= roles.size();
            for(i=0;i<n;i++)
            {
                if(roles.get(i).getIdLong()==role)
                    return true;
            }
        }
        return false;
    }

    /**
     * remove a role to the modrole list
     * also updates remote database
     * @param roleId role to remove
     * @return self object, null on error
     */
    public BotGuild removeModRole(Long roleId)
    {
        if(modRolesById.contains(roleId))
        {
            Statement stmt ;
            try {
                stmt = conn.createStatement();
                stmt.execute("DELETE FROM roles WHERE guildid="+guildId+" AND roleid="+roleId);
                stmt.execute("COMMIT");
                this.modRolesById.remove(roleId);
                stmt.close();
            }catch (SQLException ex) {
                System.out.println("SQLException: " + ex.getMessage());
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("VendorError: " + ex.getErrorCode());
                return null;
            }
        }else
            return null;
        return this;
    }

    /**
     * add a role to the modrole list
     * also updates remote database
     * @param roleId id of role to add
     * @param roleName common name of the role to add
     * @return self object, null if error
     */
    public BotGuild addModRole(Long roleId,String roleName)
    {
        if(!modRolesById.contains(roleId))
        {
            Statement stmt;
            try {
                stmt = conn.createStatement();
                stmt.execute("INSERT INTO roles (guildid,roleid,rolename) VALUES ("+guildId+","+roleId+",'"+roleName+"')");
                stmt.execute("COMMIT");
                this.modRolesById.add(roleId);
                stmt.close();
            }catch (SQLException ex) {
                System.out.println("SQLException: " + ex.getMessage());
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("VendorError: " + ex.getErrorCode());
                return null;
            }
        }else
            return null;
        return this;
    }

    public BotGuild clearModrole()
    {
        Statement stmt;
        try{
            stmt = conn.createStatement();
            stmt.execute("DELETE FROM roles WHERE guildid="+guildId);
            stmt.execute("COMMIT ");
            stmt.close();
            modRolesById.clear();
        }catch (SQLException ex)
        {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            return null;
        }
        return this;
    }

    public BotGuild autoModrole(Guild guild)
    {
        autoModRole(guild);
        return this;
    }

    public BotGuild addRoleGroup(Role role,String groupName)
    {
        if(RoleGroup.findGroup(this.roleGroups,groupName)==null)
        {
            roleGroups.add(new RoleGroup(conn,this,role,groupName));
        }else
            return null;
        return this;
    }

    public BotGuild removeRoleGroup(String groupName)
    {
        RoleGroup role = RoleGroup.findGroup(this.roleGroups,groupName);
        if(role!=null)
        {
            role.delete();
            roleGroups.remove(role);
        }else
            return null;
        return this;
    }

    public BotGuild optionRoleGroup(String groupName,String[] args,Message message,MessageChannel channel)
    {
        ResourceBundle output = getMessages();
        RoleGroup role = RoleGroup.findGroup(this.roleGroups,groupName);
        if(role!=null)
        {
            String ret = role.modify(args,message);
            channel.sendMessage(ret).queue();
        }else {
            channel.sendMessage(output.getString("error-rolegroup-not-exist")).queue();
            System.out.print("rolegroup - not found ");
            return null;
        }
        return this;
    }

    public void updateEmojiGuild(List<EmojiGuild> emojiGuilds)
    {
        for (EmojiGuild emojiGuild : emojiGuilds)
        {
            if(guildId.equals(emojiGuild.guildId)) {
                this.emojiGuild = emojiGuild;
                break;
            }
        }
    }

    /**
     * constructor of object
     * test the remote database to see if the guild already exist
     * get all informations if yes
     * create records of it otherwise
     * @param guild the guild class of api
     * @param actconn the db connection
     */
    BotGuild(Guild guild, Connection actconn,List<EmojiGuild> emojiGuilds)
    {
        String guildName = guild.getName();
        Long guildId = guild.getIdLong();
        this.conn = actconn;
        this.modRolesById = new ArrayList<>();
        this.roleGroups = new ArrayList<>();
        this.guildId = guildId;
        this.locale = new Locale("en","US");

        Statement stmt;
        ResultSet rs;
        List<Long> to_remove = new ArrayList<>();
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT guildid,prefix FROM guilds WHERE guildid=" + guildId);

            if (rs.next()) {
                this.isNew=false;
                this.prefix = rs.getString(2).intern();
                rs.close();
                rs = stmt.executeQuery("SELECT roleid FROM Roles WHERE guildid=" + guildId);
                this.modRolesById.clear();
                while (rs.next()) {
                    if(guild.getRoleById(rs.getLong(1))!=null)
                        this.modRolesById.add(rs.getLong(1));
                    else {
                        to_remove.add(rs.getLong(1));
                        MyListener.deleted=true;
                    }
                }
                rs.close();
                for(Long roleId : to_remove)
                {
                    stmt.execute("DELETE FROM roles WHERE roleid="+roleId);
                    stmt.execute("COMMIT ");
                }
                rs = stmt.executeQuery("SELECT groupid,groupname FROM groups WHERE guildid=" + guildId);

                while (rs.next()) {
                    this.roleGroups.add(new RoleGroup(conn,guild,this,rs.getLong(1),rs.getString(2)));
                }
                rs.close();
                stmt.execute("UPDATE guilds SET guildname='"+ guildName +"' WHERE guildid=" + guildId);
                stmt.execute("COMMIT");
                updateEmojiGuild(emojiGuilds);
            } else {
                rs.close();
                this.modRolesById.clear();
                this.prefix = System.getenv("DEFAULT_PREFIX");
                String emoji_prefix = System.getenv("DEFAULT_EMOJI_PREFIX");
                int max_emoji = Integer.parseInt(System.getenv("DEFAULT_MAX_EMOJI"));
                stmt.execute("INSERT INTO guilds(guildid,prefix,guildname,emoji_prefix,max_emoji) VALUES (" + this.guildId + ",'" + this.prefix + "','"+ guildName +"','"+ emoji_prefix +"',"+max_emoji+")");
                stmt.execute("COMMIT");
                EmojiGuild actual = new EmojiGuild(conn,guildId,emoji_prefix,max_emoji);
                emojiGuilds.add(actual);
                this.emojiGuild = actual;
                autoModRole(guild);
            }
            stmt.close();
            this.isOpen = true;
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }

    private void autoModRole(Guild guild)
    {
        Statement stmt;
        for (Role role : guild.getRoles())
        {
            if(role.isManaged())
                continue;
            if(role.hasPermission(Permission.ADMINISTRATOR) ||
                    role.hasPermission(Permission.MANAGE_SERVER) ||
                    role.hasPermission(Permission.MANAGE_ROLES))
                try {
                    stmt = conn.createStatement();
                    stmt.execute("INSERT INTO roles (guildid,roleid,rolename) VALUES ("+guildId+","+role.getIdLong()+",'"+role.getName()+"')");
                    stmt.execute("COMMIT");
                    this.modRolesById.add(role.getIdLong());
                    stmt.close();
                }catch (SQLException ex) {
                    System.out.println("SQLException: " + ex.getMessage());
                    System.out.println("SQLState: " + ex.getSQLState());
                    System.out.println("VendorError: " + ex.getErrorCode());
                }
        }
    }

    public boolean onRoleDeleted(Role role)
    {
        boolean ret=false;
        List<Long> to_remove = new ArrayList<>();
        for (Long roleId : modRolesById)
        {
            if(roleId.equals(role.getIdLong())) {
                to_remove.add(roleId);
                ret=true;
            }
        }
        for (Long roleId : to_remove)
            removeModRole(roleId);

        for (RoleGroup group : roleGroups)
        {
            if(group.onRoleDeleted(role))
                ret = true;
        }
        return ret;
    }

    public void close()
    {
        this.modRolesById.clear();
        this.roleGroups.clear();
        this.roleGroups=null;
        this.modRolesById=null;
        this.prefix=null;
        this.guildId=null;
        this.isOpen=false;
    }

}
