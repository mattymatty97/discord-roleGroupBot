import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.core.entities.*;
import org.jetbrains.annotations.NotNull;

public class BotGuild {
    private boolean isOpen;
    private Connection conn;
    private Long id;
    private String prefix;
    private List<Long> modRolesById;
    private boolean modified;


    public Long getId()
    {
        if(!isOpen)
            return null;
        return id;
    }

    public String getPrefix()
    {
        if(!isOpen)
            return null;
        return prefix;
    }

    public List<Long> getModRolesById() {
        if(!isOpen)
            return null;
        return modRolesById;
    }

    public BotGuild setPrefix(String n_prefix)
    {
        Statement stmt;
        try {
            stmt = conn.createStatement();
            stmt.execute("UPDATE Guilds SET Prefix='"+ n_prefix +"' WHERE GuildId="+this.id);
            stmt.execute("COMMIT");
            this.prefix = n_prefix.intern();
            stmt.close();
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return this;
    }

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

    public BotGuild removeModRole(Long roleId)
    {
        if(modRolesById.contains(roleId))
        {
            Statement stmt ;
            try {
                stmt = conn.createStatement();
                stmt.execute("DELETE FROM Roles WHERE GuildId="+id+" AND RoleId="+roleId);
                stmt.execute("COMMIT");
                this.modRolesById.remove(roleId);
                stmt.close();
            }catch (SQLException ex) {
                System.out.println("SQLException: " + ex.getMessage());
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("VendorError: " + ex.getErrorCode());
            }
        }
        return this;
    }
    public BotGuild addModRole(Long roleId,String roleName)
    {
        if(!modRolesById.contains(roleId))
        {
            Statement stmt;
            try {
                stmt = conn.createStatement();
                stmt.execute("INSERT INTO Roles (GuildId,RoleId,RoleName) VALUES ("+id+","+roleId+",'!"+roleName+"')");
                stmt.execute("COMMIT");
                this.modRolesById.add(roleId);
                stmt.close();
            }catch (SQLException ex) {
                System.out.println("SQLException: " + ex.getMessage());
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("VendorError: " + ex.getErrorCode());
            }
        }
        return this;
    }

    public BotGuild(Long guildId, String guildName, Connection actconn)
    {
        this.conn = actconn;
        this.modRolesById = new ArrayList<Long>();
        this.id = guildId;
        Statement stmt;
        ResultSet rs;
        modified = false;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT GuildId,Prefix FROM Guilds WHERE GuildId=" + guildId);

            if (rs.next()) {
                this.prefix = rs.getString(2).intern();
                rs.close();
                rs = stmt.executeQuery("SELECT RoleId FROM Roles WHERE GuildId=" + guildId);
                this.modRolesById.clear();
                while (rs.next()) {
                    this.modRolesById.add(rs.getLong(1));
                }
                rs.close();
                stmt.execute("UPDATE Guilds SET GuildName='"+ guildName +"' WHERE GuildId=" + guildId);
                stmt.execute("COMMIT");
            } else {
                this.modRolesById.clear();
                this.prefix = "tb!";
                stmt.execute("INSERT INTO Guilds(GuildId,Prefix,GuildName) VALUES (" + guildId + ",'" + prefix + "','"+guildName+"')");
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
        this.prefix=null;
        this.id=null;
        this.modified=false;
        this.isOpen=false;
    }

}
