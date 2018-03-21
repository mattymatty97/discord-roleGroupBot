package com.testBot;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.core.entities.*;

public class RoleGroup {

    private Connection conn;
    private List<Long> rolesById;
    private String type;
    private Long boundRole;
    private BotGuild guild;
    private Long groupId;

    public List<Long> getRolesById() {
        return rolesById;
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


    public RoleGroup(Connection conn, BotGuild guild,Long groupId) {
        this.conn = conn;
        this.guild = guild;
        this.groupId=groupId;
        this.rolesById = new ArrayList<>();
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
                    rs = stmt.executeQuery("SELECT roleid FROM grouproles WHERE groupid=" + groupId);
                    this.rolesById.clear();
                    while (rs.next()) {
                        this.rolesById.add(rs.getLong(1));
                    }
                    rs.close();
                } else {
                    this.rolesById.clear();
                    System.out.println("error id not found");
                }
            }else{
                stmt.execute("INSERT INTO groups (guildid,type) VALUES ("+guildId+",'LIST')");
                stmt.execute("COMMIT");
            }
            stmt.close();
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }




    }
}
