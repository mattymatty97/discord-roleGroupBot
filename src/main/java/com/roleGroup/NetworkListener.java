package com.roleGroup;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("Duplicates")
public class NetworkListener implements Runnable {
    private JDA api;
    private Connection conn;

    static boolean alive = true;

    private static Socket socket;
    private static Thread thread;

    public NetworkListener(JDA api, Connection conn) {
        this.api = api;
        this.conn = conn;
    }

    public static void close() {
        try {
            if (thread != null)
                thread.interrupt();
            if (socket != null)
                socket.close();
        } catch (IOException ignored) {
        }
    }

    ExecutorService handler = Executors.newCachedThreadPool();
    ExecutorService sender = Executors.newSingleThreadExecutor((r) -> {
        Thread t = new Thread(r, "Sender");
        t.setPriority(Thread.MAX_PRIORITY);
        return t;
    });

    private JSONObject handleAction(JSONObject request) {
        JSONObject ret;
        if (request.has("GUILD_ID")) {
            Guild guild = api.getGuildById(request.getLong("GUILD_ID"));
            BotGuild botGuild = new BotGuild(guild, conn);
            if (guild != null) {
                if (request.has("USER_ID")) {
                    Member member = guild.getMemberById(request.getLong("USER_ID"));
                    if (request.getJSONObject("ACTION").getString("ACTION").equals("user")) {
                        ret = getAuth(member, request.getLong("USER_ID"), botGuild);
                    } else if (member != null && (botGuild.memberIsMod(member) || RoleGroup.memberIsOwner(member))) {
                        switch (request.getString("TARGET")) {
                            case "guild": {
                                ret = guildAction(guild, request.getJSONObject("ACTION"));
                                break;
                            }
                            case "group": {
                                if (request.has("GROUP_ID")) {
                                    try {
                                        RoleGroup roleGroup = RoleGroup.getRolegroup(guild, conn, request.getLong("GROUP_ID"));
                                        ret = groupAction(guild, roleGroup, request.getJSONObject("ACTION"));
                                    } catch (RoleGroup.RoleGroupExeption ex) {
                                        ret = getBadAnswer(404, "RoleGroup Not Found");
                                    }
                                } else {
                                    ret = getBadAnswer(400, "Missing GROUP_ID");
                                }
                                break;
                            }
                            case "create": {
                                ret = createAction(guild, request.getJSONObject("ACTION"));
                                break;
                            }
                            default:
                                ret = getBadAnswer(400, "Unknown TARGET");
                        }
                    } else
                        ret = getBadAnswer(403, "User Not Allowed");
                } else
                    ret = getBadAnswer(400, "Missing USER_ID");
            } else {
                ret = getBadAnswer(404, "Guild Not Found");
            }
        } else {
            ret = getBadAnswer(400, "Missing GUILD_ID");
        }
        return ret;
    }

    private JSONObject handleAuth(JSONObject req) {
        JSONObject act = req.getJSONObject("ACTION");
        if (act.has("ACTION") && req.has("USER_ID")) {
            if (act.getString("ACTION").equals("server")) {
                long id = req.getLong("USER_ID");
                User user = api.getUserById(id);
                if (user != null) {
                    JSONArray server = new JSONArray();
                    for (Guild guild : api.getMutualGuilds(user)) {
                        server.put(guildToJSON(guild));
                    }
                    return getAnswer(200, "auth", new JSONObject().put("SERVERS", server));
                } else {
                    return getBadAnswer(400, "User Not FOund");
                }
            } else {
                return getBadAnswer(400, "ACTION not found");
            }
        } else {
            return getBadAnswer(400, "Missing ACTION or USER_ID");
        }
    }

    private JSONObject getAuth(Member member, long id, BotGuild botGuild) {
        JSONObject ret;
        JSONObject answer = new JSONObject();
        answer.put("MEMBER", member != null);
        boolean owner = RoleGroup.userIsOwner(id);
        if (member != null) {
            boolean mod = botGuild.memberIsMod(member);
            answer.put("ALLOWED", owner || mod);
            answer.put("MOD", mod);
            answer.put("OWNER", owner);
            answer.put("NAME", member.getEffectiveName());
            ret = getAnswer(200, "user", answer);
        } else {
            if (owner) {
                answer.put("ALLOWED", true);
                answer.put("MOD", false);
                answer.put("OWNER", true);
                answer.put("NAME", "");
                ret = getAnswer(200, "user", answer);
            } else
                answer.put("ALLOWED", false);
            answer.put("MOD", false);
            answer.put("OWNER", false);
            answer.put("NAME", "");
            ret = getAnswer(200, "user", answer);
        }
        return ret;
    }


    private JSONObject guildAction(Guild guild, JSONObject action) {
        JSONObject answer;
        if (action.has("ACTION")) {
            switch (action.getString("ACTION").toLowerCase()) {
                case "add modrole": {
                    if (action.has("ROLE_ID")) {
                        Role role = guild.getRoleById(action.getLong("ROLE_ID"));
                        if (role != null) {
                            BotGuild botGuild = new BotGuild(guild, conn);
                            if (botGuild.addModRole(role.getIdLong(), role.getName()) != null)
                                answer = getAnswer(200, "ACTION", new JSONObject().put("RESULT", "Role added"));
                            else
                                answer = getBadAnswer(400, "Role Is Modrole");
                        } else {
                            answer = getBadAnswer(400, "Role Not Found");
                        }
                    } else
                        answer = getBadAnswer(400, "Missing ROLE_ID");
                    break;
                }
                case "remove modrole": {
                    if (action.has("ROLE_ID")) {
                        Role role = guild.getRoleById(action.getLong("ROLE_ID"));
                        if (role != null) {
                            BotGuild botGuild = new BotGuild(guild, conn);
                            if (botGuild.removeModRole(role.getIdLong()) != null)
                                answer = getAnswer(200, "ACTION", new JSONObject().put("RESULT", "Role removed"));
                            else
                                answer = getBadAnswer(400, "Role Not Modrole");
                        } else {
                            answer = getBadAnswer(400, "Role Not Found");
                        }
                    } else
                        answer = getBadAnswer(400, "Missing ROLE_ID");
                    break;
                }
                case "clear modrole": {
                    BotGuild botGuild = new BotGuild(guild, conn);
                    if (botGuild.clearModrole() != null)
                        answer = getAnswer(200, "ACTION", new JSONObject().put("RESULT", "Cleared"));
                    else
                        answer = getBadAnswer(500, "Execution exception");
                    break;
                }
                case "auto modrole": {
                    BotGuild botGuild = new BotGuild(guild, conn);
                    if (botGuild.autoModrole(guild) != null)
                        answer = getAnswer(200, "ACTION", new JSONObject().put("RESULT", "Roles Added"));
                    else
                        answer = getBadAnswer(500, "Execution exception");
                    break;
                }
                default:
                    answer = getBadAnswer(404, "Unknown action");
            }
        } else
            answer = getBadAnswer(400, "Missing ACTION");
        return answer;
    }

    private JSONObject createAction(Guild guild, JSONObject action) {
        if (action.has("NAME")) {
            String name = action.getString("NAME");
            if (name.matches("\\w+")) {
                //call the class method
                if (MyListener.nameIsAllowed(name.toLowerCase())) {
                    RoleGroup rg = RoleGroup.createRolegroup(guild, name, conn);
                    if (rg != null) {
                        return getAnswer(200, "ACTION", getGroupInfo(rg));
                    } else {
                        return getBadAnswer(400, "Existing rolegroup");
                    }
                } else {
                    return getBadAnswer(400, "NAME cannot be used");
                }
            } else {
                return getBadAnswer(400, "Bad NAME format");
            }
        } else {
            return getBadAnswer(400, "Missing NAME");
        }
    }

    private JSONObject groupAction(Guild guild, RoleGroup group, JSONObject action) {
        JSONObject answer;
        if (action.has("ACTION")) {
            switch (action.getString("ACTION").toLowerCase()) {
                case "add role": {
                    if (action.has("ROLE_ID") && action.has("NICK")) {
                        Role role = guild.getRoleById(action.getLong("ROLE_ID"));
                        if (role != null) {
                            String nick = action.getString("NICK");
                            ResourceBundle out = new BotGuild(guild, conn).getMessages();
                            String res = group.addRole(role, nick, out);
                            if (res.equals(out.getString("rolegroup-role-added")))
                                answer = getAnswer(200, "ACTION", new JSONObject().put("RESULT", "Role added"));
                            else
                                answer = getBadAnswer(400, res);
                        } else {
                            answer = getBadAnswer(400, "Role Not Found");
                        }
                    } else
                        answer = getBadAnswer(400, "Missing ROLE_ID");
                    break;
                }

                case "remove role": {
                    if (action.has("NICK")) {
                        String nick = action.getString("NICK");
                        BotGuild botGuild = new BotGuild(guild, conn);
                        String res = group.removeRole(nick, botGuild.getMessages());
                        if (res.equals(botGuild.getMessages().getString("rolegroup-role-removed")))
                            answer = getAnswer(200, "ACTION", new JSONObject().put("RESULT", "Role removed"));
                        else
                            answer = getBadAnswer(400, res);
                    } else
                        answer = getBadAnswer(400, "Missing ROLE_ID");
                    break;
                }

                case "reset role": {
                    BotGuild botGuild = new BotGuild(guild, conn);
                    String res = group.resetRole(botGuild.getMessages());
                    if (res.equals(botGuild.getMessages().getString("rolegroup-role-removed")))
                        answer = getAnswer(200, "ACTION", new JSONObject().put("RESULT", "Reset"));
                    else
                        answer = getBadAnswer(500, res);
                    break;
                }

                case "type": {
                    if (action.has("TYPE")) {
                        try {
                            String type = action.getString("TYPE");
                            BotGuild botGuild = new BotGuild(guild, conn);
                            String res = group.setType(RoleGroup.Type.valueOf(type), botGuild.getMessages());
                            if (res.equals(botGuild.getMessages().getString("rolegroup-type-updated")))
                                answer = getAnswer(200, "ACTION", new JSONObject().put("RESULT", "Type set"));
                            else
                                answer = getBadAnswer(400, res);
                        } catch (IllegalArgumentException ex) {
                            answer = getBadAnswer(400, "Unknown TYPE");
                        }
                    } else
                        answer = getBadAnswer(400, "Missing ROLE_ID");
                    break;
                }

                case "expression": {
                    if (action.has("EXPRESSION")) {
                        String expression = action.getString("EXPRESSION");
                        BotGuild botGuild = new BotGuild(guild, conn);
                        String res = group.setTriggerExpr(expression, botGuild.getMessages());
                        if (res.equals(botGuild.getMessages().getString("rolegroup-expression-updated")))
                            answer = getAnswer(200, "ACTION", new JSONObject().put("RESULT", "Expression set"));
                        else
                            answer = getBadAnswer(400, res);
                    } else
                        answer = getBadAnswer(400, "Missing ROLE_ID");
                    break;
                }

                case "enable": {
                    BotGuild botGuild = new BotGuild(guild, conn);
                    String res = group.resetRole(botGuild.getMessages());
                    if (res.equals(botGuild.getMessages().getString("rolegroup-enabled")))
                        answer = getAnswer(200, "ACTION", new JSONObject().put("RESULT", "Enabled"));
                    else
                        answer = getBadAnswer(500, res);
                    break;
                }

                case "disable": {
                    BotGuild botGuild = new BotGuild(guild, conn);
                    String res = group.disable(botGuild.getMessages());
                    if (res.equals(botGuild.getMessages().getString("rolegroup-disabled")))
                        answer = getAnswer(200, "ACTION", new JSONObject().put("RESULT", "Disabled"));
                    else
                        answer = getBadAnswer(500, res);
                    break;
                }
                case "delete": {
                    group.delete();
                    answer = getAnswer(200, "ACTION", new JSONObject().put("RESULT", "Deleted"));
                    break;
                }
                default:
                    answer = getBadAnswer(404, "Unknown action");
            }
        } else
            answer = getBadAnswer(400, "Missing ACTION");
        return answer;
    }


    private JSONObject getGuildInfo(Guild guild) {
        BotGuild botGuild = new BotGuild(guild, conn);
        JSONObject res = new JSONObject();
        res.put("Name", guild.getName());
        res.put("ID", guild.getId());
        JSONArray modroles = new JSONArray();
        for (Long id : botGuild.getModRolesById()) {
            Role role = guild.getRoleById(id);
            modroles.put(roleToJSON(role));
        }
        res.put("MODROLES", modroles);
        JSONArray rolegroups = new JSONArray();
        for (String rgName : RoleGroup.listRoleGroups(guild, conn, false)) {
            RoleGroup roleGroup = RoleGroup.getRolegroup(guild, conn, rgName);
            assert roleGroup != null : "Rolegroup NULL";
            rolegroups.put(new JSONObject().put("NAME", rgName).put("ENABLED", roleGroup.isEnabled()).put("ID", Long.toString(roleGroup.getId())));
        }
        res.put("ROLEGROUPS", rolegroups);
        JSONArray roles = new JSONArray();
        for (Role role : guild.getRoles()) {
            roles.put(roleToJSON(role));
        }
        res.put("ROLES", roles);
        return res;
    }

    private JSONObject getGroupInfo(RoleGroup rg) {
        JSONObject res = new JSONObject();
        res.put("NAME", rg.getName());
        res.put("ID", Long.toString(rg.getId()));
        res.put("TYPE", rg.getType().toString());

        JSONArray triggerroles = new JSONArray();
        if (rg.getTriggerRoleMap().size() > 0)
            rg.getTriggerRoleMap().forEach((i, r) -> triggerroles
                    .put(new JSONObject()
                            .put("BIND", "$" + i)
                            .put("NAME", (r != null) ? r.getName() : "deleted")
                            .put("ID", (r != null) ? r.getId() : "0")));


        res.put("EXPRESSION", new JSONObject()
                .put("TEXT", rg.getTriggerExpr())
                .put("ROLES", triggerroles));

        JSONArray roles = new JSONArray();
        if (rg.getRoleMap().size() > 0)
            rg.getRoleMap().forEach((key, role) -> roles.put(new JSONObject().put("NICK", key)
                    .put("ROLE", roleToJSON(role))));
        res.put("ROLES", roles);
        res.put("ENABLED", rg.isEnabled());
        return res;
    }


    private JSONObject getAnswer(int status, String type, JSONObject rep) {
        JSONObject answer = new JSONObject();
        answer.put("ID", "rolegroup");
        answer.put("STATUS", status);
        answer.put("TYPE", type);
        answer.put("CONTENT", rep);
        return answer;
    }

    private JSONObject getBadAnswer(int code) {
        JSONObject answer = new JSONObject();
        answer.put("ID", "rolegroup");
        answer.put("STATUS", code);
        return answer;
    }

    private JSONObject getBadAnswer(int code, String reason) {
        JSONObject answer = new JSONObject();
        answer.put("ID", "rolegroup");
        answer.put("STATUS", code);
        answer.put("REASON", reason);
        return answer;
    }

    private JSONObject roleToJSON(Role role) {
        return new JSONObject()
                .put("ID", role.getId())
                .put("NAME", role.getName())
                .put("COLOR", role.getColorRaw())
                .put("MANAGED", role.isManaged())
                .put("MENTIONABLE", role.isMentionable())
                .put("EVERYONE", role.isPublicRole());
    }

    private JSONObject guildToJSON(Guild guild) {
        return new JSONObject()
                .put("ID", guild.getId())
                .put("NAME", guild.getName())
                .put("ICON", guild.getIconUrl());
    }

    private DataOutputStream outToServer;
    private DataInputStream inFromServer;

    private String handleMessage(String message) {
        JSONObject request = new JSONObject(message);
        JSONObject answer;

        Thread.currentThread().setName("Handle message<" + request.getInt("ReqID") + ">");

        System.out.print("\n" + Thread.currentThread().getName() + " | ");
        System.out.println("WEB - Received:");
        System.out.println(request.toString(3));

        if (request.has("REQUEST")) {
            switch (request.getString("REQUEST")) {
                case "ping": {
                    JSONObject ret = new JSONObject();
                    ret.put("VALUE", "pong");
                    answer = getAnswer(200, "String", ret);
                    break;
                }
                case "guild": {
                    if (request.has("GUILD_ID")) {
                        Guild guild = api.getGuildById(request.getLong("GUILD_ID"));
                        if (guild != null)
                            answer = getAnswer(200, "Guild", getGuildInfo(guild));
                        else
                            answer = getBadAnswer(404, "Guild Not Found");
                    } else {
                        answer = getBadAnswer(400, "missing GUILD_ID");
                    }
                    break;
                }
                case "group": {
                    if (request.has("GUILD_ID") && request.has("GROUP_ID")) {
                        Guild guild = api.getGuildById(request.getLong("GUILD_ID"));
                        if (guild != null) {
                            try {
                                RoleGroup roleGroup = RoleGroup.getRolegroup(guild, conn, request.getLong("GROUP_ID"));
                                answer = getAnswer(200, "RoleGroup", getGroupInfo(roleGroup));
                            } catch (RoleGroup.RoleGroupExeption ex) {
                                answer = getBadAnswer(404, "RoleGroup Not Found");
                            }
                        } else
                            answer = getBadAnswer(404, "Guild Not Found");
                    } else {
                        answer = getBadAnswer(400, "Missing GUILD_ID or GROUP_ID");
                    }
                    break;
                }
                case "action": {
                    if (request.has("TARGET") && request.has("ACTION")) {
                        answer = handleAction(request);
                    } else {
                        answer = getBadAnswer(400, "Missing TARGET or ACTION");
                    }
                    break;
                }
                case "auth": {
                    if (request.has("ACTION"))
                        answer = handleAuth(request);
                    else
                        answer = getBadAnswer(400, "Missing ACTION");
                }
                break;
                default: {
                    answer = getBadAnswer(404, "Unknown request");
                }
            }
        } else {
            answer = getBadAnswer(400);
        }


        JSONObject printRep = new JSONObject().put("ID", answer.get("ID")).put("STATUS", answer.get("STATUS"));
        System.out.print("\n" + Thread.currentThread().getName() + " | ");
        System.out.println("WEB - Answered:");
        System.out.println(printRep.toString(3));

        answer.put("ReqID", request.getInt("ReqID"));
        return answer.toString();
    }
    @Override
    public void run() {
        thread = Thread.currentThread();
        thread.setName("Receiver");
        thread.setPriority(Thread.NORM_PRIORITY + 1);
        while (!thread.isInterrupted())
            try {
                while (!thread.isInterrupted()) {
                    socket = new Socket("torino.ddns.net", 23446);
                    outToServer = new DataOutputStream(socket.getOutputStream());
                    inFromServer = new DataInputStream(socket.getInputStream());
                    outToServer.writeUTF("rolegroup");
                    outToServer.flush();
                    System.out.println("Rest API started");
                    alive = true;

                    receive();

                    socket.close();
                }
            } catch (IOException ex) {
                if (alive)
                    System.err.println("Rest API dead");
                alive = false;
            }
    }

    public void receive() throws IOException {
        while (!socket.isClosed()) {
            String message = inFromServer.readUTF();
            handler.execute(() -> sender.execute(() -> send(handleMessage(message))));
        }
    }

    public void send(String message) {
        try {
            outToServer.writeUTF(message);
            outToServer.flush();
        }
        catch (IOException ex) {
            if (alive)
                System.err.println("Rest API dead");
            alive = false;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }
}
