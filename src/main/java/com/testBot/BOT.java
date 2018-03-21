package com.testBot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.testBot.BotGuild;
import com.testBot.MyListener;

import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class BOT
{
    static String url;
    public static void main(String[] arguments) throws Exception
    {
        Connection conn=null;

        List<BotGuild> savedGuilds=new ArrayList<BotGuild>();
        try {
            //Class.forName("com.mysql.jdbc.Driver");
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            //System.out.println("Missing MySQL JDBC Driver!");
            System.out.println("Missing postgresql JDBC Driver!");
            e.printStackTrace();
            return;
        }
        try {
            url = "jdbc:postgresql://ec2-54-247-81-88.eu-west-1.compute.amazonaws.com/d5a36f2opkuv4a";
            Properties props = new Properties();
            props.setProperty("user","hmefwffiqybapa");
            props.setProperty("password","c054e4e3cbca7e7695c3b87d104e87777ee952fde4891516236f074e0e4fb7ec");
            props.setProperty("ssl","true");
            props.setProperty("sslfactory","org.postgresql.ssl.NonValidatingFactory");
            conn = DriverManager.getConnection(url, props);
            //conn = DriverManager.getConnection("jdbc:mysql://84.201.37.95/Bot?user=Bot&password=BotPassword");
            System.out.println("SQL INITIALIZZATED");
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }

        JDA api = new JDABuilder(AccountType.BOT).setToken("NDIwNTY0MTU1NjQ5NjIyMDE4.DYA5dA._MmdVLt7jHqwlJpbEI4YE07ULxs").buildAsync();
        api.addEventListener(new MyListener(conn,savedGuilds));
        api.getPresence().setGame(Game.playing("\"testing the bot :/\""));
    }
}