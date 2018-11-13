package com.roleGroup;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;

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
                            answer=getBadAnswer(404,"Guild Not Found");
                    }else{
                        answer = getBadAnswer(400,"missing GUILD_ID");
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
                                answer=getBadAnswer(404,"RoleGroup Not Found");
                            }
                        }else
                            answer=getBadAnswer(404,"Guild Not Found");
                    }else{
                        answer = getBadAnswer(400,"Missing GUILD_ID or GROUP_ID");
                    }
                    break;
                }
                case "action":{
                    if(request.has("TARGET") && request.has("ACTION")){
                        answer = handleAction(request);
                    }else{
                        answer = getBadAnswer(400,"Missing TARGET or ACTION");
                    }
                    break;
                }
                default: {
                    answer = getBadAnswer(404,"Unknown request");
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

    private JSONObject handleAction(JSONObject request){
        JSONObject ret;
        switch (request.getString("TARGET")){
            case "guild":{
                if(request.has("GUILD_ID")) {
                    Guild guild = api.getGuildById(request.getLong("GUILD_ID"));
                    if(guild!=null)
                        ret = guildAction(guild,request.getJSONObject("ACTION"));
                    else
                        ret=getBadAnswer(404,"Guild Not Found");
                }else{
                    ret = getBadAnswer(400,"Missing GUILD_ID");
                }
                break;
            }
            case "group":{
                ret = getBadAnswer(500);
                break;
            }
            default:
                ret = getBadAnswer(400,"Unknown TARGET");
        }
        return ret;
    }




    private JSONObject guildAction(Guild guild, JSONObject action){
        JSONObject answer;
        if(action.has("ACTION")){
            switch (action.getString("ACTION").toLowerCase()){
                case "add modrole":{
                    if(action.has("ROLE_ID")){
                        Role role = guild.getRoleById(action.getLong("ROLE_ID"));
                        if(role!=null){
                            BotGuild botGuild = new BotGuild(guild,conn);
                            if(botGuild.addModRole(role.getIdLong(),role.getName())!=null)
                                answer = getAnswer(200,"ACTION",new JSONObject().put("RESULT","Role added"));
                            else
                                answer = getBadAnswer(400,"Role Is Modrole");
                        }else {
                            answer = getBadAnswer(400,"Role Not Found");
                        }
                    }else
                        answer = getBadAnswer(400,"Missing ROLE_ID");
                    break;
                }
                case "remove modrole":{
                    if(action.has("ROLE_ID")){
                        Role role = guild.getRoleById(action.getLong("ROLE_ID"));
                        if(role!=null){
                            BotGuild botGuild = new BotGuild(guild,conn);
                            if(botGuild.removeModRole(role.getIdLong())!=null)
                                answer = getAnswer(200,"ACTION",new JSONObject().put("RESULT","Role removed"));
                            else
                                answer = getBadAnswer(400,"Role Not Modrole");
                        }else {
                            answer = getBadAnswer(400,"Role Not Found");
                        }
                    }else
                        answer = getBadAnswer(400,"Missing ROLE_ID");
                    break;
                }
                case "clear modrole":{
                    BotGuild botGuild = new BotGuild(guild,conn);
                    if(botGuild.clearModrole()!=null)
                        answer = getAnswer(200,"ACTION",new JSONObject().put("RESULT","Cleared"));
                    else
                        answer = getBadAnswer(500,"Execution exception");
                    break;
                }
                case "auto modrole":{
                    BotGuild botGuild = new BotGuild(guild,conn);
                    if(botGuild.autoModrole(guild)!=null)
                        answer = getAnswer(200,"ACTION",new JSONObject().put("RESULT","Roles Added"));
                    else
                        answer = getBadAnswer(500,"Execution exception");
                    break;
                }
                default:
                    answer = getBadAnswer(404,"Unknown action");
            }
        }else
            answer = getBadAnswer(400,"Missing ACTION");
        return answer;
    }


    private JSONObject getGuildInfo(Guild guild){
        BotGuild botGuild = new BotGuild(guild,conn);
        JSONObject res = new JSONObject();
        res.put("Name", guild.getName());
        res.put("ID", guild.getIdLong());
        JSONArray modroles = new JSONArray();
        for(Long id : botGuild.getModRolesById()){
            Role role = guild.getRoleById(id);
            modroles.put(new JSONObject().put("NAME",role.getName()).put("ID",role.getIdLong()));
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

        JSONArray triggerroles =new JSONArray();
        rg.getTriggerRoleMap().forEach((i,r)->triggerroles
                .put(new JSONObject()
                        .put("BIND","$"+i)
                        .put("NAME",r.getName())
                        .put("ID",r.getIdLong())));


        res.put("EXPRESSION",new JSONObject()
                .put("TEXT",rg.getTriggerExpr())
                .put("ROLES",triggerroles));

        JSONArray roles = new JSONArray();
        rg.getRoleMap().forEach((key, role) -> roles.put(new JSONObject().put("NICK", key)
                                                    .put("ROLE", new JSONObject()
                                                                .put("NAME", role.getName())
                                                                .put("ID", role.getIdLong())
                                                    )));
        res.put("ROLES",roles);
        res.put("ENABLED",rg.isEnabled());
        return res;
    }



    private JSONObject getAnswer(int status,String type,JSONObject rep){
        JSONObject answer = new JSONObject();
        answer.put("ID","rolegroup");
        answer.put("STATUS",status);
        answer.put("TYPE",type);
        answer.put("CONTENT",rep);
        return answer;
    }

    private JSONObject getBadAnswer(int code){
        JSONObject answer = new JSONObject();
        answer.put("ID","rolegroup");
        answer.put("STATUS",code);
        return answer;
    }

    private JSONObject getBadAnswer(int code,String reason){
        JSONObject answer = new JSONObject();
        answer.put("ID","rolegroup");
        answer.put("STATUS",code);
        answer.put("REASON",reason);
        return answer;
    }
}
