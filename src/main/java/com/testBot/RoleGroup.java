package com.testBot;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.dv8tion.jda.core.entities.*;

public class RoleGroup {

    private Connection conn;
    private List<RoleData> roles;
    private String type;
    private Long boundRole;
    private BotGuild guild;
    private Long groupId;
    private String groupName;

    public List<RoleData> getRoles() {
        return roles;
    }

    public String getType() {
        return type;
    }

    public Long getBoundRole() {
        return boundRole;
    }

    public BotGuild getGuild() {
        return guild;
    }

    public String getGroupName() {
        return groupName;
    }

    public String[] modify(String[] args,MessageChannel channel)
    {
        switch (args[0])
        {
            case "add":

                break;

            case "remove":

                break;

            case "type":

                break;

            default:

                break;

        }








        return null;
    }

    public RoleGroup(Connection conn, BotGuild guild, Long groupId, String groupName) {
        this.conn = conn;
        this.guild = guild;
        this.groupId=groupId;
        this.groupName = groupName;
        this.roles = new ArrayList<>();
        Long guildId = guild.getId();
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            if(groupId!=null){
                rs = stmt.executeQuery("SELECT type,roleid FROM groups WHERE groupid=" + groupId);

                if (rs.next()) {
                    this.type = rs.getString(1);
                    this.boundRole = rs.getLong(2);
                    rs.close();
                    rs = stmt.executeQuery("SELECT roleid,rolename FROM grouproles WHERE groupid=" + groupId);
                    this.roles.clear();
                    while (rs.next()) {
                        this.roles.add(new RoleData(rs.getString(1),rs.getLong(2)));
                    }
                    rs.close();
                } else {
                    this.roles.clear();
                    System.out.println("error id not found");
                }
            }else{
                stmt.execute("INSERT INTO groups (guildid,groupname,type) VALUES ("+guildId+",'"+groupName+"','LIST')");
                stmt.execute("COMMIT");
            }
            stmt.close();
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }

    public void delete()
    {
        Statement stmt;
        try {
            stmt = conn.createStatement();
            stmt.execute("DELETE FROM grouproles WHERE groupid="+groupId);
            stmt.execute("DELETE FROM groups WHERE groupid="+groupId);
            stmt.execute("COMMIT");
            stmt.close();
            conn=null;
            roles=null;
            type=null;
            boundRole=null;
            guild=null;
            groupId=null;
            groupName=null;

        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoleGroup)) return false;
        RoleGroup roleGroup = (RoleGroup) o;
        return Objects.equals(guild, roleGroup.guild) &&
                Objects.equals(groupName, roleGroup.groupName);
    }

    public static RoleGroup findGroup(List<RoleGroup> list,String groupName)
    {
        for (RoleGroup group : list) {
            if(group.getGroupName().equals(groupName))
                return group;
        }
        return null;
    }
}
