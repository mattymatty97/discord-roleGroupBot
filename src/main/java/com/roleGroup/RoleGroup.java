package com.roleGroup;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;

import java.sql.*;
import java.util.*;

@SuppressWarnings({"WeakerAccess","Duplicates","unused"})
public class RoleGroup {

    private Connection conn;

    private String name;
    private long id;
    private boolean enabled;

    private Map<String, Role> roleMap = new HashMap<>();
    private Role triggerRole;

    private Type type = Type.LIST;


    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, Role> getRoleMap() {
        return new HashMap<>(roleMap);
    }

    public Role getTriggerRole() {
        return triggerRole;
    }

    public Type getType() {
        return type;
    }


    public String command(Guild guild, Member member, String rolename, ResourceBundle output) {
        Role role;
        StringBuilder ret = new StringBuilder();
        if(rolename==null){
            if(!type.isStrict()) {
                ret.append(output.getString("cc-list-shown")).append("\n");
                for (String name : this.roleMap.keySet()) {
                    ret.append(name).append("\n");
                }
            }else{
                ret.append("```diff\n").append(output.getString("cc-list-shown")).append("\n");
                for (Map.Entry<String,Role> entry : this.roleMap.entrySet()) {
                    String sign = (roleIsUsed(entry.getValue()))?"-":"+";
                    ret.append(sign).append(" ").append(entry.getKey()).append("\n");
                }
                ret.append("```");
            }
            System.out.print("grouproles custom - list shown");
        }else {
            if (enabled) {
                switch (type) {
                    case LIST:
                        role = roleMap.get(rolename);
                        if (role == null) {
                            System.out.print("grouproles custom - wrong syntax");
                            ret.append(output.getString("error-wrong-syntax"));
                        } else {
                            if (guild.getSelfMember().getRoles().get(0).getPosition() > role.getPosition()) {
                                if (member.getRoles().contains(role)) {
                                    guild.getController().removeRolesFromMember(member, role).queue();
                                    ret.append(output.getString("cc-role-removed").replace("{role}", role.getName()));
                                    System.out.print("grouproles custom - role removed");
                                } else {
                                    guild.getController().addRolesToMember(member, role).queue();
                                    ret.append(output.getString("cc-role-added").replace("{role}", role.getName()));
                                    System.out.print("grouproles custom - role added");
                                }
                            } else {
                                ret.append(output.getString("error-bot-permission"));
                                System.out.print("grouproles custom - too low role");
                            }
                        }
                        break;
                    case POOL:
                        role = roleMap.get(rolename);
                        if (role == null) {
                            System.out.print("grouproles custom - wrong syntax");
                            ret.append(output.getString("error-wrong-syntax"));
                        } else {
                            if (guild.getSelfMember().getRoles().get(0).getPosition() > role.getPosition()) {
                                if (member.getRoles().contains(role)) {
                                    guild.getController().removeRolesFromMember(member, role).queue();
                                    ret.append(output.getString("cc-role-removed").replace("{role}", role.getAsMention()));
                                    System.out.print("grouproles custom - role removed");
                                } else if (guild.getMembers().stream().noneMatch((Member m) -> m.getRoles().contains(role))) {
                                    guild.getController().addRolesToMember(member, role).queue();
                                    ret.append(output.getString("cc-role-added").replace("{role}", role.getAsMention()));
                                    System.out.print("grouproles custom - role added");
                                } else {
                                    ret.append(output.getString("cc-role-pool-used").replace("{role}", role.getName()));
                                    System.out.print("grouproles custom - pool role yet used");
                                }
                            } else {
                                ret.append(output.getString("error-bot-permission"));
                                System.out.print("grouproles custom - too low role");
                            }
                        }
                        break;
                    case MONO:
                        role = roleMap.get(rolename);
                        if (role == null) {
                            System.out.print("grouproles custom - wrong syntax");
                            ret.append(output.getString("error-wrong-syntax"));
                        } else {
                            if (guild.getSelfMember().getRoles().get(0).getPosition() > role.getPosition()) {
                                if (member.getRoles().contains(role)) {
                                    guild.getController().removeRolesFromMember(member, role).queue();
                                    ret.append(output.getString("cc-role-removed").replace("{role}", role.getName()));
                                    System.out.print("grouproles custom - role removed");
                                } else {
                                    List<Role> to_remove = new LinkedList<>();
                                    List<Role> to_add = new LinkedList<>();
                                    to_add.add(role);
                                    member.getRoles().stream().filter(r -> roleMap.values().contains(r)).filter(r -> !role.equals(r)).forEach(to_remove::add);
                                    guild.getController().modifyMemberRoles(member, to_add, to_remove).queue();
                                    to_remove.forEach(r -> {
                                        ret.append(output.getString("cc-role-removed").replace("{role}", r.getName())).append("\n");
                                    });
                                    ret.append(output.getString("cc-role-added").replace("{role}", role.getName()));
                                    System.out.print("grouproles custom - role substituted");
                                }
                            } else {
                                ret.append(output.getString("error-bot-permission"));
                                System.out.print("grouproles custom - too low role");
                            }
                        }
                        break;
                    case MONOPOOL:
                        role = roleMap.get(rolename);
                        if (role == null) {
                            System.out.print("grouproles custom - wrong syntax");
                            ret.append(output.getString("error-wrong-syntax"));
                        } else {
                            if (guild.getSelfMember().getRoles().get(0).getPosition() > role.getPosition()) {
                                if (member.getRoles().contains(role)) {
                                    guild.getController().removeRolesFromMember(member, role).queue();
                                    ret.append(output.getString("cc-role-removed").replace("{role}", role.getAsMention()));
                                    System.out.print("grouproles custom - role removed");
                                } else if (guild.getMembers().stream().noneMatch((Member m) -> m.getRoles().contains(role))) {
                                    List<Role> to_remove = new LinkedList<>();
                                    List<Role> to_add = new LinkedList<>();
                                    to_add.add(role);
                                    member.getRoles().stream().filter(r -> roleMap.values().contains(r)).filter(r -> !role.equals(r)).forEach(to_remove::add);
                                    guild.getController().modifyMemberRoles(member, to_add, to_remove).queue();
                                    to_remove.forEach(r -> {
                                        ret.append(output.getString("cc-role-removed").replace("{role}", r.getAsMention())).append("\n");
                                    });
                                    ret.append(output.getString("cc-role-added").replace("{role}", role.getAsMention()));
                                    System.out.print("grouproles custom - role substituted");
                                } else {
                                    ret.append(output.getString("cc-role-pool-used").replace("{role}", role.getName()));
                                    System.out.print("grouproles custom - pool role yet used");
                                }
                            } else {
                                ret.append(output.getString("error-bot-permission"));
                                System.out.print("grouproles custom - too low role");
                            }
                        }
                        break;
                }
            } else {
                ret.append(output.getString("error-cc-disabled"));
                System.out.print("grouproles custom - command disabled");
            }
        }
        return ret.toString();
    }

    public boolean memberAllowed(Member member) {
        return member.getRoles().contains(triggerRole);
    }


    public String addRole(Role role, String roleName, ResourceBundle output) {
        StringBuilder retStr = new StringBuilder();
        PreparedStatement stmt;
        if (!enabled) {
            if (roleName.length() <= 10) {
                if (!roleMap.containsValue(role)) {
                    if (!roleMap.containsKey(roleName)) {
                        try {
                            stmt = conn.prepareStatement("INSERT INTO grouproles(groupid, roleid, rolename) VALUES (?,?,?)");
                            stmt.setLong(1, this.id);
                            stmt.setLong(2, role.getIdLong());
                            stmt.setString(3, roleName);
                            stmt.executeUpdate();
                            stmt.close();
                            this.roleMap.put(roleName, role);
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
            System.out.print("grouproles - command enabled not modify");
            retStr.append(output.getString("error-rolegroup-modify"));
        }
        return retStr.toString();
    }

    public String removeRole(String roleName, ResourceBundle output) {
        StringBuilder retStr = new StringBuilder();
        PreparedStatement stmt;
        if (!enabled) {
            Role role = this.roleMap.get(roleName);
            if (role != null) {
                try {
                    stmt = conn.prepareStatement("DELETE FROM grouproles WHERE groupid=? AND roleid=?");
                    stmt.setLong(1, id);
                    stmt.setLong(2, role.getIdLong());
                    stmt.executeUpdate();
                    stmt.close();
                    this.roleMap.remove(roleName);
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
            System.out.print("grouproles - command enabled not modify");
            retStr.append(output.getString("error-rolegroup-modify"));
        }
        return retStr.toString();
    }

    public String resetRole(ResourceBundle output) {
        StringBuilder retStr = new StringBuilder();
        PreparedStatement stmt;
        ResultSet rs;
        if (!enabled) {
            try {
                stmt = conn.prepareStatement("DELETE FROM grouproles WHERE groupid=?");
                stmt.setLong(1, id);
                stmt.executeUpdate();
                stmt.close();
                this.roleMap.clear();
                System.out.println("grouproles - role removed");
                retStr.append(output.getString("rolegroup-role-removed"));
            } catch (SQLException ex) {
                System.out.println("SQLException: " + ex.getMessage());
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("VendorError: " + ex.getErrorCode());
                System.out.print("grouproles - error on role");
                retStr.append(output.getString("error-rolegroup-reset"));
            }
        } else {
            System.out.print("grouproles - command enabled not modify");
            retStr.append(output.getString("error-rolegroup-modify"));
        }
        return retStr.toString();
    }

    public String setType(Type type, ResourceBundle output) {
        PreparedStatement stmt;
        StringBuilder retStr = new StringBuilder();
        if (!isEnabled()) {
            try {
                stmt = conn.prepareStatement("UPDATE groups SET type=? WHERE groupid=?");
                stmt.setString(1, type.toString());
                stmt.setLong(2, id);
                stmt.executeUpdate();
                stmt.close();
                this.type = type;
                System.out.print("grouproles - type udated");
                retStr.append(output.getString("rolegroup-type-updated"));
            } catch (SQLException ex) {
                System.out.println("SQLException: " + ex.getMessage());
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("VendorError: " + ex.getErrorCode());
                System.out.print("grouproles - error in type");
                retStr.append(output.getString("error-rolegroup-type"));
            }
        } else {
            System.out.print("grouproles - command enabled not modify");
            retStr.append(output.getString("error-rolegroup-modify"));
        }
        return retStr.toString();
    }

    public String setTriggerRole(Role role, ResourceBundle output) {
        PreparedStatement stmt;
        StringBuilder retStr = new StringBuilder();
        try {
            stmt = conn.prepareStatement("UPDATE groups SET roleid=? WHERE groupid=?");
            stmt.setLong(1, role.getIdLong());
            stmt.setLong(2, id);
            stmt.executeUpdate();
            this.triggerRole = role;
            retStr.append(output.getString("rolegroup-boundrole-updated"));
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            retStr.append(output.getString("error-rolegroup-boundrole"));
        }
        return retStr.toString();
    }

    public MessageEmbed printStatus(ResourceBundle output) {
        EmbedBuilder ret = new EmbedBuilder();

        ret.setTitle(output.getString("rolegroup-status-title").replace("{group}", name));
        ret.setDescription(output.getString("rolegroup-status-enabled") +" **" + (enabled) + "**\n" );
        ret.appendDescription(output.getString("rolegroup-status-boundrole"));

        if (triggerRole != null) {
            ret.appendDescription(triggerRole.getAsMention());
        }
        ret.appendDescription("\n");
        ret.appendDescription(output.getString("rolegroup-status-type") +" **"+ type + "**\n");
        ret.appendDescription(output.getString("rolegroup-status-role") + "\n");
        for (Map.Entry<String, Role> entry : roleMap.entrySet()) {
            ret.appendDescription(entry.getValue().getAsMention());
            ret.appendDescription(" " + output.getString("rolegroup-status-as") + " **");
            ret.appendDescription(entry.getKey() + "**\n");
        }
        System.out.print("grouproles - showing status of " + name);

        return ret.build();
    }

    public String enable(ResourceBundle output) {
        Statement stmt;
        StringBuilder retStr = new StringBuilder();
        if (triggerRole != null) {
            if (!enabled) {
                if(roleMap.size()>0) {
                    try {
                        stmt = conn.createStatement();
                        stmt.execute("UPDATE groups SET enabled=TRUE WHERE groupid=" + id);
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
                    System.out.print("grouproles - error empty rolegroup");
                    retStr.append(output.getString("error-rolegroup-empty"));
                }
            } else {
                System.out.print("grouproles - error yet enabled");
                retStr.append(output.getString("error-rolegroup-is-enable"));
            }
        } else {
            System.out.print("grouproles - missing boundrole");
            retStr.append(output.getString("error-rolegroup-bound"));
        }
        return retStr.toString();
    }

    public String disable(ResourceBundle output) {
        Statement stmt;
        StringBuilder retStr = new StringBuilder();
        if (enabled) {
            try {
                stmt = conn.createStatement();
                stmt.execute("UPDATE groups SET enabled=FALSE WHERE groupid=" + id);
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
        } else {
            System.out.print("grouproles - error yet disabled");
            retStr.append(output.getString("error-rolegroup-is-disable"));
        }
        return retStr.toString();
    }

    public void delete(){
        Statement stmt;
        try {
            stmt = conn.createStatement();
            stmt.execute("DELETE FROM grouproles WHERE groupid="+id);
            stmt.execute("DELETE FROM groups WHERE groupid="+id);
            stmt.execute("COMMIT");
            stmt.close();
            conn=null;
            roleMap=null;
            type=null;
            triggerRole=null;
            id=0;
            name=null;
            enabled=false;
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }


    private RoleGroup(String name, Role boundrole, Connection conn) {
        this.conn = conn;

        this.name = name;
        this.triggerRole = boundrole;

        Long guildId = boundrole.getGuild().getIdLong();
        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            stmt.execute("INSERT INTO groups (guildid,groupname,type,roleid) VALUES (" + guildId + ",'" + name + "','LIST'," + boundrole.getIdLong() + ")");
            rs = stmt.executeQuery("SELECT groupid FROM groups WHERE guildid=" + guildId + " AND groupname='" + name + "'");
            if (rs.next())
                this.id = rs.getLong(1);
            stmt.execute("COMMIT");
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }

    private RoleGroup(Guild guild, long groupId, Connection conn) {
        this.conn = conn;
        this.id = groupId;

        Statement stmt;
        ResultSet rs;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT groupname,type,roleid,enabled FROM groups WHERE groupid=" + groupId);

            if (rs.next()) {
                this.name = rs.getString("groupname");
                this.type = Type.valueOf(rs.getString("type").toUpperCase());
                this.triggerRole = guild.getRoleById(rs.getLong("roleid"));
                this.enabled = rs.getBoolean("enabled");
                rs.close();
                rs = stmt.executeQuery("SELECT roleid,rolename FROM grouproles WHERE groupid=" + groupId);
                while (rs.next()) {
                    this.roleMap.put(rs.getString("rolename"), guild.getRoleById(rs.getLong("roleid")));
                }
                rs.close();

            } else {
                System.out.println("error id not found");
            }
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }


    public static RoleGroup getRolegroup(Guild guild, Connection conn, String groupName) {
        RoleGroup ret;
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT groupid FROM groups WHERE guildid=? AND groupname=?");
            ResultSet rs;

            stmt.setLong(1, guild.getIdLong());
            stmt.setString(2, groupName);

            rs = stmt.executeQuery();

            if (!rs.next())
                ret = null;
            else
                ret = new RoleGroup(guild, rs.getLong("groupid"), conn);

            rs.close();
            stmt.close();
            return ret;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            System.out.print("grouproles - error getting informations ");
            return null;
        }
    }

    public static RoleGroup createRolegroup(String name, Role boundrole, Connection conn) {
        RoleGroup ret;
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT groupid FROM groups WHERE guildid=? AND groupname=?");
            ResultSet rs;

            stmt.setLong(1, boundrole.getGuild().getIdLong());
            stmt.setString(2, name);

            rs = stmt.executeQuery();

            if (rs.next())
                ret = null;
            else
                ret = new RoleGroup(name, boundrole, conn);

            rs.close();
            stmt.close();
            return ret;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            System.out.print("grouproles - error getting informations ");
            return null;
        }
    }

    public static List<String> listRoleGroups(Guild guild,Connection conn){
        Statement stmt;
        ResultSet rs;
        List<String> ret = new ArrayList<>();
        try{
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT groupname,enabled FROM groups WHERE guildid="+guild.getId());
            while (rs.next()){
                ret.add((rs.getBoolean("enabled")?"+":"-") + rs.getString("groupname"));
            }
            rs.close();
            stmt.close();
        }catch (SQLException ex) {
                System.out.println("SQLException: " + ex.getMessage());
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("VendorError: " + ex.getErrorCode());
                System.out.print("grouproles - error getting informations ");
        }
        return ret;
    }

    public static void onRoleDeletion(Role role,Connection conn){
        PreparedStatement stmt;
        ResultSet rs;
        try{
            stmt = conn.prepareStatement("SELECT groupid from grouproles where roleid=?");
            stmt.setLong(1,role.getIdLong());
            rs = stmt.executeQuery();
            List<Long> ids = new LinkedList<>();
            while (rs.next()){
                ids.add(rs.getLong("groupid"));
            }
            rs.close();
            stmt.close();
            if(ids.size()>0) {
                stmt = conn.prepareStatement("UPDATE groups SET enabled=FALSE WHERE groupid=?");
                for (Long id : ids) {
                    stmt.setLong(1, id);
                    stmt.executeUpdate();
                }
                stmt.close();
                stmt = conn.prepareStatement("DELETE FROM grouproles where roleid=?");
                stmt.setLong(1, role.getIdLong());
                stmt.executeUpdate();
            }
        }catch (SQLException ex){
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            System.out.print("SQL error on role deletion ");
        }
    }



    public enum Type {
        POOL("POOL"),
        LIST("LIST"),
        MONO("MONO"),
        MONOPOOL("MONOPOOL");

        private String string;

        public boolean isStrict(){

            if ( this.equals(POOL) )
                return true;
            if ( this.equals(MONOPOOL))
                return true;

            return false;
        }

        @Override
        public String toString() {

            return this.string;

        }

        Type(String string) {
            this.string = string;
        }
    }


    private boolean memberHasRole(Member member, Long roleId) {
        List<Role> list = member.getRoles();
        Role role = member.getGuild().getRoleById(roleId);
        return role != null && list.contains(role);
    }

    private boolean roleIsUsed(Role role){
        return role.getGuild().getMembers().stream().map(Member::getRoles).flatMap(List::stream).anyMatch(role::equals);
    }

}
