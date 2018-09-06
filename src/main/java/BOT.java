import com.roleGroup.*;

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

        api.addEventListener(new MyListener(conn,savedGuilds));
        api.getPresence().setGame(Game.playing("v1.0 rg prj"));
    }


}
