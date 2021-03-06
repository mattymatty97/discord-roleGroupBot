package com.roleGroup;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Reconnector {
    public static void reconnect(){
        boolean connected=false;
        Connection conn = null;

        List<BotGuild> savedGuilds = new ArrayList<BotGuild>();
        while (!connected) {
            try {
                TimeUnit.SECONDS.sleep(5);
            }catch(Exception e){
                e.printStackTrace();
            }
            System.out.println("trying to reconnect to sql");
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                System.out.println("Missing postgresql JDBC Driver!");
                e.printStackTrace();
                connected = false;
                continue;
            }
            try {
                URI dbUri;
                try {
                    dbUri = new URI(System.getenv("DATABASE_URL"));
                }catch (URISyntaxException ex)
                {
                    System.out.println("URIException: " + ex.getMessage());
                    System.out.println("Reason: " + ex.getReason());
                    continue;
                }

                String username = dbUri.getUserInfo().split(":")[0];
                String password = dbUri.getUserInfo().split(":")[1];
                String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() + "?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory&" + "user=" + username + "&password=" + password;
                System.out.println("Connecting to: " + dbUrl);
                conn = DriverManager.getConnection(dbUrl);
                System.out.println("SQL INITIALIZZATED");
                connected = true;
            } catch (SQLException ex) {
                System.out.println("NOT CONNECTED RETRY IN 5 SEC");
                connected = false;
            }
        }
        try {
            JDA api = JDABuilder.createLight(System.getenv("BOT_TOKEN")).build();
            api.addEventListener(new MyListener(conn));
            api.getPresence().setPresence(Activity.listening("suggestions :/"), false);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
