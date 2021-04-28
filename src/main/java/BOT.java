import com.roleGroup.MyListener;
import com.roleGroup.NetworkListener;
import com.roleGroup.SupportListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class BOT
{
    static String url;
    public static void main(String[] arguments) throws Exception
    {
        Connection conn=null;
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

        JDA api = JDABuilder.createLight(System.getenv("BOT_TOKEN")).build();

        api.addEventListener(new MyListener(conn));
        api.addEventListener(new SupportListener(491954204106031104L));
        api.getPresence().setPresence(Activity.playing("v3.0 rg prj"), false);

        while (!MyListener.ready);

        NetworkListener listener = new NetworkListener(api, conn);
        listener.run();
        Runtime.getRuntime().addShutdownHook(new Thread(NetworkListener::close));
    }


}
