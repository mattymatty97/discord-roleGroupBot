import com.testBot.*;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import java.sql.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.core.entities.Game;

public class BOT
{
    static String url;
    public static void main(String[] arguments) throws Exception
    {
        Connection conn=null;

        List<BotGuild> savedGuilds=new ArrayList<BotGuild>();
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Missing postgresql JDBC Driver!");
            e.printStackTrace();
            return;
        }
        try {
            URI dbUri = new URI(System.getenv("DATABASE_URL"));

            String username = dbUri.getUserInfo().split(":")[0];
            String password = dbUri.getUserInfo().split(":")[1];
            String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath()+"?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory&"+"user="+username+"&password="+password;
            System.out.println("Connecting to: "+ dbUrl);
            conn = DriverManager.getConnection(dbUrl);
            System.out.println("SQL INITIALIZZATED");
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            System.exit(-1);
        }

        JDA api = new JDABuilder(AccountType.BOT).setToken(System.getenv("BOT_TOKEN")).buildAsync();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT guildid FROM guilds");
            List<Long> to_remove = new ArrayList<>();
            while (rs.next())
            {
                if(api.getGuildById(rs.getLong(1))==null)
                {
                    to_remove.add(rs.getLong(1));
                }
            }
            rs.close();
            stmt.close();
            for (Long guildId : to_remove)
                guildDeleteDB(conn,guildId);
        }catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }

        api.addEventListener(new MyListener(conn,savedGuilds));
        api.getPresence().setGame(Game.playing("v1.1"));
    }

    private static void guildDeleteDB(Connection conn,Long guildId)
    {
        try{
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM guilds WHERE guildid="+guildId);
            if(rs.next())
            {
                rs.close();
                rs = stmt.executeQuery("SELECT groupid FROM groups WHERE guildid="+guildId);
                while(rs.next())
                {
                    stmt.execute("DELETE FROM grouproles WHERE groupid="+rs.getLong(1));
                }
                rs.close();
                stmt.execute("DELETE FROM groups WHERE guildid="+guildId);
                stmt.execute("DELETE FROM roles WHERE guildid="+guildId);
            }else {
                rs.close();
            }
            stmt.execute("DELETE FROM guilds WHERE guildid="+guildId);
            stmt.execute("COMMIT");
            stmt.close();
        }catch (SQLException ex)
        {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }

    public static void reconnect()
    {

    }
}
