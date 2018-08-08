package com.testBot;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import net.dv8tion.jda.core.entities.*;

public class RoleGroup {

    private Connection conn;
    private List<RoleData> roles;
    private String type;
    private Long boundRole;
    private BotGuild guild;
    private Long groupId;
    private String groupName;
    private boolean enabled;

    public boolean isEnabled(){
        return enabled;
    }

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



    public String command(Guild guild, Member member, String rolename,ResourceBundle output)
    {
        RoleData rd;
        StringBuilder ret = new StringBuilder();
        if(isEnabled()){
            switch (type)
            {
                case "LIST":
                    rd = RoleData.find(roles,rolename);
                    if(rd==null)
                    {
                        System.out.print("grouproles custom - wrong syntax");
                        ret.append(output.getString("error-wrong-syntax"));
                    }else {
                        Role role = guild.getRoleById(rd.getRoleId());
                        if (guild.getSelfMember().getRoles().get(0).getPosition() > role.getPosition()) {
                            if (memberHasRole(member, rd.getRoleId())) {
                                guild.getController().removeRolesFromMember(member, role).queue();
                                ret.append(output.getString("cc-role-removed").replace("{role}",role.getName()));
                                System.out.print("grouproles custom - role removed");
                            } else {
                                guild.getController().addRolesToMember(member, role).queue();
                                ret.append(output.getString("cc-role-added").replace("{role}",role.getName()));
                                System.out.print("grouproles custom - role added");
                            }
                        }else{
                            ret.append(output.getString("error-bot-permission"));
                            System.out.print("grouproles custom - too low role");
                        }
                    }
                    break;
                case "POOL":
                    rd = RoleData.find(roles,rolename);
                    if(rd==null)
                    {
                        System.out.print("grouproles custom - wrong syntax");
                        ret.append(output.getString("error-wrong-syntax"));
                    }else {
                        Role role = guild.getRoleById(rd.getRoleId());
                        if (guild.getSelfMember().getRoles().get(0).getPosition() > role.getPosition()) {
                                if (memberHasRole(member, rd.getRoleId())) {
                                    guild.getController().removeRolesFromMember(member, role).queue();
                                    ret.append(output.getString("cc-role-removed").replace("{role}", role.getName()));
                                    System.out.print("grouproles custom - role removed");
                                } else if (guild.getMembers().stream().noneMatch((Member m)->m.getRoles().contains(role))){
                                    guild.getController().addRolesToMember(member, role).queue();
                                    ret.append(output.getString("cc-role-added").replace("{role}", role.getName()));
                                    System.out.print("grouproles custom - role added");
                                } else {
                                    ret.append(output.getString("cc-role-pool-used").replace("{role}", role.getName()));
                                    System.out.print("grouproles custom - pool role yet used");
                                }
                        }else{
                            ret.append(output.getString("error-bot-permission"));
                            System.out.print("grouproles custom - too low role");
                        }
                    }
                    break;
            }
        }else{
            ret.append(output.getString("error-cc-disabled"));
            System.out.print("grouproles custom - command disabled");
        }

        return ret.toString();
    }

    public String modify(String[] args,Message message)
    {
        ResourceBundle output = guild.getMessages();
        Statement stmt;
        StringBuilder retStr = new StringBuilder();
        switch (args[0])
        {
            case "add":
                if(!isEnabled()) {
                    //get mention list
                    List<Role> list = message.getMentionedRoles();
                    //if there is a mention and the syintax is correct
                    if (list.size() == 1 && args.length == 4 && args[2].equals("as")) {
                        //if the name is not too long
                        if (args[3].length() <= 10) {
                            if (RoleData.find(roles, list.get(0).getIdLong()) == null) {
                                if (RoleData.find(roles, args[3]) == null) {
                                    try {
                                        stmt = conn.createStatement();
                                        Long id = list.get(0).getIdLong();
                                        stmt.execute("INSERT INTO grouproles(groupid, roleid, rolename) VALUES (" + this.groupId + "," + id + ",'" + args[3] + "')");
                                        stmt.execute("COMMIT");
                                        roles.add(new RoleData(args[3], list.get(0).getIdLong()));
                                        stmt.close();
                                        retStr.append(output.getString("rolegroup-role-added"));
                                        System.out.print("grouproles - role added");
                                    } catch (SQLException ex) {
                                        System.out.println("SQLException: " + ex.getMessage());
                                        System.out.println("SQLState: " + ex.getSQLState());
                                        System.out.println("VendorError: " + ex.getErrorCode());
                                        retStr.append(output.getString("error-rolegroup-add"));
                                        System.out.print("grouproles - error on role ");
                                    }
                                } else {
                                    System.out.print("grouproles - found existing nick ");
                                    retStr.append(output.getString("error-rolegroup-nick"));
                                }
                            } else {
                                System.out.print("grouproles - found existing role ");
                                retStr.append(output.getString("error-rolegroup-role-included"));
                            }
                        } else {
                            System.out.print("grouproles - name limit exceed ");
                            retStr.append(output.getString("error-rolegroup-long-nick"));
                        }
                    } else {
                        System.out.print("grouproles - wrong syntax");
                        retStr.append(output.getString("error-wrong-syntax"));
                    }
                }else{
                    System.out.print("grouproles - command enabled not modify");
                    retStr.append(output.getString("error-rolegroup-modify"));
                }
                break;

            case "remove":
                if(!isEnabled()) {
                    if (args[1] != null) {
                        RoleData role = RoleData.find(roles, args[1]);
                        if (role != null) {
                            try {
                                stmt = conn.createStatement();
                                stmt.execute("DELETE FROM grouproles WHERE groupid=" + groupId + " AND roleid=" + role.getRoleId());
                                stmt.execute("COMMIT");
                                roles.remove(role);
                                stmt.close();
                                System.out.println("grouproles - role removed");
                                retStr.append(output.getString("rolegroup-role-removed"));
                            } catch (SQLException ex) {
                                System.out.println("SQLException: " + ex.getMessage());
                                System.out.println("SQLState: " + ex.getSQLState());
                                System.out.println("VendorError: " + ex.getErrorCode());
                                System.out.print("grouproles - error on role");
                                retStr.append(output.getString("error-rolegroup-remove"));
                            }
                        } else {
                            System.out.print("grouproles - role not found");
                            retStr.append(output.getString("error-wrong-syntax"));
                        }
                    } else {
                        System.out.print("grouproles - wrong syntax");
                        retStr.append(output.getString("error-wrong-syntax"));
                    }
                }else{
                    System.out.print("grouproles - command enabled not modify");
                    retStr.append(output.getString("error-rolegroup-modify"));
                }
                break;
            case "reset":
                if(!isEnabled()) {
                    try {
                        stmt = conn.createStatement();
                        stmt.execute("DELETE FROM grouproles WHERE groupid=" + groupId);
                        stmt.execute("COMMIT");
                        roles.clear();
                        stmt.close();
                        System.out.println("grouproles - role removed");
                        retStr.append(output.getString("rolegroup-role-removed"));
                    } catch (SQLException ex) {
                        System.out.println("SQLException: " + ex.getMessage());
                        System.out.println("SQLState: " + ex.getSQLState());
                        System.out.println("VendorError: " + ex.getErrorCode());
                        System.out.print("grouproles - error on role");
                        retStr.append(output.getString("error-rolegroup-reset"));
                    }
                }else{
                    System.out.print("grouproles - command enabled not modify");
                    retStr.append(output.getString("error-rolegroup-modify"));
                }
                break;
            case "type":
                if(!isEnabled()) {
                    if (args[1] != null) {
                        switch (args[1].toLowerCase()) {
                            case "list":
                            case "pool":
                                try {
                                    stmt = conn.createStatement();
                                    stmt.execute("UPDATE groups SET type='" + args[1].toUpperCase() + "' WHERE groupid=" + groupId);
                                    stmt.execute("COMMIT");
                                    this.type = args[1].toUpperCase();
                                    stmt.close();
                                    System.out.print("grouproles - type udated");
                                    retStr.append(output.getString("rolegroup-type-updated"));
                                } catch (SQLException ex) {
                                    System.out.println("SQLException: " + ex.getMessage());
                                    System.out.println("SQLState: " + ex.getSQLState());
                                    System.out.println("VendorError: " + ex.getErrorCode());
                                    System.out.print("grouproles - error in type");
                                    retStr.append(output.getString("error-rolegroup-type"));
                                }
                                break;
                            default:
                                System.out.print("grouproles - type not found");
                                retStr.append(output.getString("error-rolegroup-404-type"));

                        }
                    } else {
                        System.out.print("grouproles - wrong syntax");
                        retStr.append(output.getString("error-wrong-syntax"));
                    }
                }else{
                    System.out.print("grouproles - command enabled not modify");
                    retStr.append(output.getString("error-rolegroup-modify"));
                }
                break;
            case "boundrole":
                List<Role> list = message.getMentionedRoles();
                //if there is a mentioned role
                if (list.size() == 1) {
                    //call the class method
                    try {
                        stmt = conn.createStatement();
                        stmt.execute("UPDATE groups SET roleid="+list.get(0).getIdLong()+" WHERE groupid="+groupId);
                        stmt.execute("COMMIT");
                        this.boundRole=list.get(0).getIdLong();
                        retStr.append(output.getString("rolegroup-boundrole-updated"));
                        stmt.close();
                    } catch (SQLException ex) {
                        System.out.println("SQLException: " + ex.getMessage());
                        System.out.println("SQLState: " + ex.getSQLState());
                        System.out.println("VendorError: " + ex.getErrorCode());
                        retStr.append(output.getString("error-rolegroup-boundrole"));
                    }
                }else
                {
                    System.out.print("wrong syntax ");
                    retStr.append(output.getString("error-wrong-syntax"));
                }
            case "status":
                {
                    Guild guild = message.getGuild();
                    retStr.append(output.getString("rolegroup-status-title").replace("{group}",groupName)).append("\n");
                    retStr.append(output.getString("rolegroup-status-enabled")).append(enabled).append("\n");
                    retStr.append(output.getString("rolegroup-status-boundrole"));
                    if(boundRole!=null)
                        retStr.append(guild.getRoleById(boundRole).getName());
                    retStr.append("\n");
                    retStr.append(output.getString("rolegroup-status-type")).append(type).append("\n");
                    retStr.append(output.getString("rolegroup-status-role")).append("\n");
                    for (RoleData role : roles)
                    {
                        retStr.append(guild.getRoleById(role.getRoleId()).getName());
                        retStr.append(" ").append(output.getString("rolegroup-status-as")).append(" ");
                        retStr.append(role.getRoleName()).append("\n");
                    }
                    System.out.print("grouproles - showing status of "+ groupName);
                }
                break;
            case "enable":
                if(boundRole!=null)
                {
                    if(!isEnabled()) {
                        try {
                            stmt = conn.createStatement();
                            stmt.execute("UPDATE groups SET enabled=TRUE WHERE groupid=" + groupId);
                            stmt.execute("COMMIT");
                            this.enabled = true;
                            stmt.close();
                            System.out.print("grouproles - group enabled");
                            retStr.append(output.getString("rolegroup-enabled"));
                        } catch (SQLException ex) {
                            System.out.println("SQLException: " + ex.getMessage());
                            System.out.println("SQLState: " + ex.getSQLState());
                            System.out.println("VendorError: " + ex.getErrorCode());
                            System.out.print("grouproles - error in enable");
                            retStr.append(output.getString("error-rolegroup-enable"));
                        }
                    }else{
                        System.out.print("grouproles - error yet enabled");
                        retStr.append(output.getString("error-rolegroup-is-enable"));
                    }
                }else{
                    System.out.print("grouproles - missing boundrole");
                    retStr.append(output.getString("error-rolegroup-bound"));
                }
                break;
            case "disable":
                if(isEnabled()) {
                    try {
                        stmt = conn.createStatement();
                        stmt.execute("UPDATE groups SET enabled=FALSE WHERE groupid=" + groupId);
                        stmt.execute("COMMIT");
                        this.enabled = false;
                        stmt.close();
                        System.out.print("grouproles - group disabled");
                        retStr.append(output.getString("rolegroup-disabled"));
                    } catch (SQLException ex) {
                        System.out.println("SQLException: " + ex.getMessage());
                        System.out.println("SQLState: " + ex.getSQLState());
                        System.out.println("VendorError: " + ex.getErrorCode());
                        System.out.print("grouproles - error in disable");
                        retStr.append(output.getString("error-rolegroup-disable"));
                    }
                }else{
                    System.out.print("grouproles - error yet disabled");
                    retStr.append(output.getString("error-rolegroup-is-disable"));
                }
                break;
            default:
                System.out.print("grouproles - wrong syntax");
                retStr.append(output.getString("error-wrong-syntax"));
                break;

        }
        return retStr.toString();
    }



    public boolean isValid()
    {
        return roles.size() > 0;
    }

    public String printHelp()
    {
        StringBuilder ret = new StringBuilder(groupName).append(" ");
        switch (type)
        {
            case "LIST":
                for(RoleData role : roles)
                {
                    ret.append(role.getRoleName()).append("/");
                }
                if(roles.size()>0)
                    ret.deleteCharAt(ret.lastIndexOf("/"));
        }
        return ret.toString();
    }




    RoleGroup(Connection conn, Guild guild, BotGuild botGuild, Long groupId, String groupName) {
        this.conn = conn;
        this.guild = botGuild;
        this.groupId=groupId;
        this.groupName = groupName;
        this.roles = new ArrayList<>();
        List<Long> to_remove = new ArrayList<>();
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
                rs = stmt.executeQuery("SELECT type,roleid FROM groups WHERE groupid=" + groupId);

                if (rs.next()) {
                    this.type = rs.getString(1);
                    this.boundRole = rs.getLong(2);
                    rs.close();
                    rs = stmt.executeQuery("SELECT roleid,rolename FROM grouproles WHERE groupid=" + groupId);
                    this.roles.clear();
                    while (rs.next()) {
                        if(guild.getRoleById(rs.getLong(1))!=null)
                            this.roles.add(new RoleData(rs.getString(2),rs.getLong(1)));
                        else
                        {
                            to_remove.add(rs.getLong(1));
                            MyListener.deleted=true;
                        }
                    }
                    rs.close();
                    for (Long roleId : to_remove)
                    {
                        stmt.execute("DELETE FROM grouproles WHERE groupid="+groupId+" AND roleid="+roleId);
                        stmt.execute("COMMIT");
                    }
                } else {
                    this.roles.clear();
                    System.out.println("error id not found");
                }
            stmt.close();
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }

    RoleGroup(Connection conn, BotGuild botGuild,Role role, String groupName) {
        this.conn = conn;
        this.guild = botGuild;
        this.groupName = groupName;
        this.roles = new ArrayList<>();
        Long guildId = botGuild.getId();
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            stmt.execute("INSERT INTO groups (guildid,groupname,type,roleid) VALUES ("+guildId+",'"+groupName+"','LIST',"+role.getIdLong()+")");
            rs = stmt.executeQuery("SELECT groupid FROM groups WHERE guildid="+guildId+" AND groupname='"+groupName+"'");
            if(rs.next())
                this.groupId = rs.getLong(1);
            stmt.execute("COMMIT");
            this.boundRole = role.getIdLong();
            stmt.close();
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }


    public boolean onRoleDeleted(Role role)
    {
        boolean ret=false;
        List<RoleData> to_remove = new ArrayList<>();
        if(boundRole.equals(role.getIdLong()))
        {
            Statement stmt;
            try {
                stmt = conn.createStatement();
                stmt.execute("UPDATE groups SET roleid=NULL WHERE groupid="+groupId);
                stmt.execute("COMMIT");
                this.boundRole=null;
                this.enabled=false;
                ret=true;
                stmt.close();
            } catch (SQLException ex) {
                System.out.println("SQLException: " + ex.getMessage());
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("VendorError: " + ex.getErrorCode());
            }
        }

        for (RoleData data : roles)
        {
            if(data.getRoleId().equals(role.getIdLong()))
            {
                to_remove.add(data);
                this.enabled = false;
            }
        }

        for (RoleData data : to_remove)
        {
            Statement stmt;
            try {
                stmt = conn.createStatement();
                stmt.execute("DELETE FROM grouproles WHERE groupid=" + groupId + " AND roleid=" + data.getRoleId());
                stmt.execute("COMMIT");
                roles.remove(data);
                stmt.close();
            } catch (SQLException ex) {
                System.out.println("SQLException: " + ex.getMessage());
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("VendorError: " + ex.getErrorCode());
            }
            ret=true;
        }
        return ret;
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

    private boolean memberHasRole(Member member,Long roleId) {
        List<Role> list = member.getRoles();
        Role role = member.getGuild().getRoleById(roleId);
        return role != null && list.contains(role);
    }

}
