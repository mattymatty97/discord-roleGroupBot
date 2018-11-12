package com.roleGroup;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.Time;
import java.util.concurrent.TimeUnit;

public class NetworkListener implements Runnable {
    private JDA api;
    private Connection conn;

    public NetworkListener(JDA api,Connection conn) {
        this.api=api;
        this.conn=conn;
    }

    static boolean alive=true;

    private long millis=0;

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                Socket socket = new Socket("torino.ddns.net", 23446);
                DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
                DataInputStream inFromServer = new DataInputStream(socket.getInputStream());
                outToServer.writeUTF("rolegroup");
                outToServer.flush();
                System.out.println("Rest API started");
                alive = true;

                while (!socket.isClosed()) {
                    if (inFromServer.available()>0) {
                        String message = inFromServer.readUTF();
                        String answer = handleMessage(message);
                        outToServer.writeUTF(answer);
                        outToServer.flush();
                        millis = System.currentTimeMillis();
                    }
                    Thread.yield();
                    if(System.currentTimeMillis() > millis+1000){
                        millis=System.currentTimeMillis();
                    }
                }
                socket.close();
            }
        }catch (IOException ex){
            if(alive)
                System.err.println("Rest API dead");
            alive=false;
            new Thread(this).start();
        }
    }

    private String handleMessage(String message){
        JSONObject request = new JSONObject(message);
        JSONObject answer;

        System.out.println("WEB - Received:");
        System.out.println(request.toString(3));

        if(request.has("REQUEST")) {
            switch (request.getString("REQUEST")) {
                case "ping": {
                    JSONObject ret = new JSONObject();
                    ret.put("VALUE", "pong");
                    answer = getAnswer(200, "String", ret);
                    break;
                }
                case "guild": {
                    if(request.has("GUILD_ID")) {
                        Guild guild = api.getGuildById(request.getLong("GUILD_ID"));
                        if(guild!=null)
                            answer = getAnswer(200,"Guild",getGuildInfo(guild));
                        else
                            answer=getBadAnswer();
                    }else{
                        answer = getBadAnswer();
                    }
                    break;
                }
                default: {
                    answer = getBadAnswer();
                }
            }
        }else{
            answer = getBadAnswer();
        }



        JSONObject printRep = new JSONObject().put("ID",answer.get("ID")).put("STATUS",answer.get("STATUS"));
        System.out.println("WEB - Answered:");
        System.out.println(printRep.toString(3));

        return answer.toString();
    }

    private JSONObject getGuildInfo(Guild guild){
        BotGuild botGuild = new BotGuild(guild,conn);
        JSONObject res = new JSONObject();
        JSONArray modroles = new JSONArray();
        for(Long id : botGuild.getModRolesById()){
            Role role = guild.getRoleById(id);
            modroles.put(new JSONObject().put("NAME",role.getName()).put("ID",role.getId()));
        }
        res.put("MODROLES",modroles);
        JSONArray rolegroups = new JSONArray();
        for(String rgName : RoleGroup.listRoleGroups(guild,conn,false)){
            RoleGroup roleGroup = RoleGroup.getRolegroup(guild,conn,rgName);
            assert roleGroup!=null : "Rolegroup NULL";
            rolegroups.put(new JSONObject().put("NAME",rgName).put("ENABLED",roleGroup.isEnabled()).put("ID",roleGroup.getId()));
        }
        res.put("ROLEGROUPS",rolegroups);
        return res;
    }






    private JSONObject getAnswer(int status,String type,JSONObject ret){
        JSONObject answer = new JSONObject();
        answer.put("ID","rolegroup");
        answer.put("STATUS",status);
        answer.put("TYPE",type);
        answer.put("CONTENT",ret);
        return answer;
    }

    private JSONObject getBadAnswer(){
        JSONObject answer = new JSONObject();
        answer.put("ID","rolegroup");
        answer.put("STATUS",400);
        return answer;
    }
}
