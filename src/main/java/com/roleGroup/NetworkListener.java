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
                        String message = inFromServer.readUTF();
                        String answer = handleMessage(message);
                        outToServer.writeUTF(answer);
                        outToServer.flush();
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
                            answer=getBadAnswer(404);
                    }else{
                        answer = getBadAnswer(400);
                    }
                    break;
                }
                case "group":{
                    if(request.has("GUILD_ID") && request.has("GROUP_ID")) {
                        Guild guild = api.getGuildById(request.getLong("GUILD_ID"));
                        if(guild!=null) {
                            try{
                                RoleGroup roleGroup = RoleGroup.getRolegroup(guild, conn, request.getLong("GROUP_ID"));
                                answer = getAnswer(200, "RoleGroup", getGroupInfo(roleGroup));
                            }catch (RoleGroup.RoleGroupExeption ex){
                                answer=getBadAnswer(404);
                            }
                        }else
                            answer=getBadAnswer(404);
                    }else{
                        answer = getBadAnswer(400);
                    }
                    break;
                }

                default: {
                    answer = getBadAnswer(400);
                }
            }
        }else{
            answer = getBadAnswer(400);
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

    private JSONObject getGroupInfo(RoleGroup rg){
        JSONObject res = new JSONObject();
        res.put("NAME",rg.getName());
        res.put("ID",rg.getId());
        res.put("TYPE",rg.getType().toString());
        res.put("EXPRESSION",rg.getPrintableTriggerExpr());
        JSONArray roles = new JSONArray();
        rg.getRoleMap().entrySet().forEach(e -> {
            Role role = e.getValue();
            roles.put(new JSONObject().put("NICK",e.getKey())
                    .put("ROLE",
                            new JSONObject()
                                    .put("NAME",role.getName())
                                    .put("ID",role.getId())));
        });
        res.put("ROLES",roles);
        res.put("ENABLED",rg.isEnabled());

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

    private JSONObject getBadAnswer(int code){
        JSONObject answer = new JSONObject();
        answer.put("ID","rolegroup");
        answer.put("STATUS",code);
        return answer;
    }
}
