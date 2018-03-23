package com.testBot;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.core.entities.*;

/**
 * @author mattymatty
 * local class for storing per guild informations.
 */
public class BotGuild {
    private boolean isOpen;         /**bool value to test validity of object**/
    private Connection conn;        /**SQL connection object send by main**/
    private Long id;                /**guild id used to identify guild**/
    private String prefix;          /**prefix for trig a reaction**/
    private List<Long> modRolesById;/**list of roles (stored by id) that are allowed to run mod commands**/
    private List<RoleGroup> roleGroups;
    private boolean modified;       /**STILL UNUSED**/

    /**
     * getter to id attribute
     * @return the guild id in format Long
     */
    public Long getId()
    {
        if(!isOpen)
            return null;
        return id;
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
            stmt.execute("UPDATE guilds SET prefix='"+ n_prefix +"' WHERE guildId="+this.id);
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
                stmt.execute("DELETE FROM roles WHERE guildid="+id+" AND roleid="+roleId);
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
                stmt.execute("INSERT INTO roles (guildid,roleid,rolename) VALUES ("+id+","+roleId+",'"+roleName+"')");
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
        RoleGroup role = RoleGroup.findGroup(this.roleGroups,groupName);
        if(role!=null)
        {
            String ret = role.modify(args,message);
            channel.sendMessage(ret).queue();
        }else {
            channel.sendMessage("rolegroup not found!").queue();
            System.out.print("rolegroup - not found ");
            return null;
        }
        return this;
    }


    /**
     * constructor of object
     * test the remote database to see if the guild already exist
     * get all informations if yes
     * create records of it otherwise
     * @param guildId the id of the guild
     * @param guildName the common name of the guild
     * @param actconn the db connection
     */
    public BotGuild(Long guildId, String guildName, Connection actconn)
    {
        this.conn = actconn;
        this.modRolesById = new ArrayList<Long>();
        this.roleGroups = new ArrayList<>();
        this.id = guildId;
        Statement stmt;
        ResultSet rs;
        modified = false;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT guildid,prefix FROM guilds WHERE guildid=" + guildId);

            if (rs.next()) {
                this.prefix = rs.getString(2).intern();
                rs.close();
                rs = stmt.executeQuery("SELECT roleid FROM Roles WHERE guildid=" + guildId);
                this.modRolesById.clear();
                while (rs.next()) {
                    this.modRolesById.add(rs.getLong(1));
                }
                rs.close();
                rs = stmt.executeQuery("SELECT groupid,groupname FROM groups WHERE guildid=" + guildId);

                while (rs.next()) {
                    this.roleGroups.add(new RoleGroup(conn,this,rs.getLong(1),rs.getString(2)));
                }
                rs.close();
                stmt.execute("UPDATE guilds SET guildname='"+ guildName +"' WHERE guildid=" + guildId);
                stmt.execute("COMMIT");
            } else {
                this.modRolesById.clear();
                this.prefix = "tb!";
                stmt.execute("INSERT INTO guilds(guildid,prefix,guildname) VALUES (" + guildId + ",'" + prefix + "','"+guildName+"')");
                stmt.execute("COMMIT");
            }
            stmt.close();
            this.isOpen = true;
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }


    public void close()
    {
        this.modRolesById.clear();
        this.roleGroups.clear();
        this.roleGroups=null;
        this.modRolesById=null;
        this.prefix=null;
        this.id=null;
        this.modified=false;
        this.isOpen=false;
    }

}
