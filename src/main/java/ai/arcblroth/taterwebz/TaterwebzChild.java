package ai.arcblroth.taterwebz;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.ProjectInceptionEarlyRiser;
import ai.arcblroth.taterwebz.util.NotKnotClassLoader;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

public class TaterwebzChild {

    public static TaterwebzOptions OPTIONS;

    public static void main(String[] args) throws IOException {
        OPTIONS = new TaterwebzOptions();

        OptionParser optionParser = new OptionParser();
        optionParser.allowsUnrecognizedOptions();
        OptionSpec<File> gameDirOption = optionParser.accepts("gameDir").withRequiredArg().ofType(File.class);
        OptionSpec<String> proxyHostOption = optionParser.accepts("proxyHost").withRequiredArg();
        OptionSpec<Integer> proxyPortOption = optionParser.accepts("proxyPort").withRequiredArg().defaultsTo("8080", new String[0]).ofType(Integer.class);
        OptionSpec<String> proxyUserOption = optionParser.accepts("proxyUser").withRequiredArg();
        OptionSpec<String> proxyPassOption = optionParser.accepts("proxyPass").withRequiredArg();
        OptionSet optionSet = optionParser.parse(args);

        String proxyHost = getOption(optionSet, proxyHostOption);
        Proxy proxy = Proxy.NO_PROXY;
        if (proxyHost != null) {
            try {
                proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, getOption(optionSet, proxyPortOption)));
                OPTIONS.proxyAddress = new InetSocketAddress(proxyHost, getOption(optionSet, proxyPortOption));
            } catch (Exception e) {}
        }

        final String proxyUser = getOption(optionSet, proxyUserOption);
        final String proxyPass = getOption(optionSet, proxyPassOption);
        if (!proxy.equals(Proxy.NO_PROXY) && isNotNullOrEmpty(proxyUser) && isNotNullOrEmpty(proxyPass)) {
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(proxyUser, proxyPass.toCharArray());
                }
            });
        }

        OPTIONS.runDirectory = optionSet.valueOf(gameDirOption).getCanonicalFile();
        OPTIONS.queueDirectory = new File(OPTIONS.runDirectory, "projectInception" + File.separator + ProjectInceptionEarlyRiser.BROWSER_PREFIX).getCanonicalFile();
        ProjectInceptionEarlyRiser.initChronicleQueues(OPTIONS.queueDirectory);

        NotKnotClassLoader classLoader = new NotKnotClassLoader(TaterwebzChild.class.getClassLoader());
        try {
            Class<?> cefDownloader = classLoader.loadClass("ai.arcblroth.taterwebz.CEFDownloader");
            Method postLaunch = cefDownloader.getMethod("onPostLaunch", NotKnotClassLoader.class);
            postLaunch.invoke(cefDownloader.newInstance(), classLoader);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    private static <T> T getOption(OptionSet optionSet, OptionSpec<T> optionSpec) {
        try {
            return optionSet.valueOf(optionSpec);
        } catch (Throwable t) {
            if (optionSpec instanceof ArgumentAcceptingOptionSpec) {
                ArgumentAcceptingOptionSpec<T> argumentAcceptingOptionSpec = (ArgumentAcceptingOptionSpec<T>)optionSpec;
                List<T> list = argumentAcceptingOptionSpec.defaultValues();
                if (!list.isEmpty()) {
                    return list.get(0);
                }
            }
            throw t;
        }
    }

    private static boolean isNotNullOrEmpty(@Nullable String s) {
        return s != null && !s.isEmpty();
    }

}
