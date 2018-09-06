import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Icon;

import java.io.File;

public class Manager {
    public static void main(String[] arguments) throws Exception{

        JDA api = new JDABuilder(AccountType.BOT).setToken(System.getenv("BOT_TOKEN")).buildBlocking();

        api.getSelfUser().getManager().setName("roleGroupBot - beta").complete();

        api.shutdown();

    }
}
