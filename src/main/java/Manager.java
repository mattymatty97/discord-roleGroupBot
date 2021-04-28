import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Icon;

import java.io.File;

public class Manager {
    public static void main(String[] arguments) throws Exception{

        JDA api = JDABuilder.createLight(System.getenv("BOT_TOKEN")).build().awaitReady();

        api.getSelfUser().getManager().setName("RoleGroup").complete();
        api.getSelfUser().getManager().setAvatar(Icon.from(new File("/home/mattymatty/Desktop/pfp/roleGroup.png"))).complete();

        api.shutdown();

    }
}
