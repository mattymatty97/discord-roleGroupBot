import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class BOT
{
    public static void main(String[] arguments) throws Exception
    {
        Connection conn=null;
        List<BotGuild> savedGuilds=new ArrayList<BotGuild>();
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Where is your MySQL JDBC Driver?");
            e.printStackTrace();
            return;
        }
        try {

            conn = DriverManager.getConnection("jdbc:mysql://84.201.37.95/Bot?user=Bot&password=BotPassword");
            System.out.println("SQL INITIALIZZATED");
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }

        JDA api = new JDABuilder(AccountType.BOT).setToken("NDIwNTY0MTU1NjQ5NjIyMDE4.DYA5dA._MmdVLt7jHqwlJpbEI4YE07ULxs").buildAsync();
        api.addEventListener(new MyListener(conn,savedGuilds));
    }
}