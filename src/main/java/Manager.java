import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Icon;

import java.io.File;

public class Manager {
    public static void main(String[] arguments) throws Exception{

        JDA api = new JDABuilder(AccountType.BOT).setToken(System.getenv("BOT_TOKEN")).build().awaitReady();

        api.getSelfUser().getManager().setName("RoleGroup").complete();
        api.getSelfUser().getManager().setAvatar(Icon.from(new File("/home/mattymatty/Desktop/pfp/roleGroup.png"))).complete();

        api.shutdown();

    }
}
