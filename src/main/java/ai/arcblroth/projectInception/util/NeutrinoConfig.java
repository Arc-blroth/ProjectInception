package ai.arcblroth.projectInception.util;

import com.google.gson.Gson;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.ClassReader.SKIP_DEBUG;
import static org.objectweb.asm.Opcodes.*;

class NeutrinoConfig {

    public static void main(String[] args) throws Exception {
        // Generate the class
        ClassWriter writer = new ClassWriter(0);
        writer.visit(V1_8, ACC_PUBLIC + ACC_ABSTRACT, "N", null, "java/io/File", null);
        ClassReader reader = new ClassReader("ai.arcblroth.projectInception.util.NeutrinoConfig");
        ClassNode srcNode = new ClassNode();
        reader.accept(srcNode, SKIP_DEBUG);
        srcNode.methods.stream().filter(m -> m.name.length() == 1).forEach(methodNode -> {
            methodNode.exceptions.clear();
            methodNode.accept(writer);
        });
        InjectedClassLoader loader = new InjectedClassLoader();
        byte[] clazzSource = writer.toByteArray();
        loader.injections.put("N", clazzSource);
        Class<?> clazz = loader.loadClass("N");
        new FileOutputStream(new File("N.class")).write(clazzSource);
        System.out.println("Wrote N.class to disk: " + clazzSource.length + " bytes total");

        // test it!
        Method l = clazz.getMethod("l", Reader.class);
        Method s = clazz.getMethod("s", Map.class, Writer.class);

        Map<String, Object> stuff = new HashMap<>();
        stuff.put("Arc'blroth presents", "Neutrino");
        stuff.put("Not a mod by", "Vazkii");
        stuff.put("Bytes", clazzSource.length);
        stuff.put("Illegal methods", 1.5);
        stuff.put("Sane", false);
        stuff.put("Useful", new String[] {"Yes", "No", "Maybe"});
        StringWriter sWriter = new StringWriter();
        s.invoke(null, stuff, sWriter);
        StringReader fReader = new StringReader(sWriter.toString());
        System.out.println(l.invoke(null, fReader));
    }

    public static Map<String, Object> l(Reader r) {
        return new Gson().fromJson(r, Map.class);
    }

    public static void s(Map<String, Object> in, Writer w) throws Exception {
        w.write(new Gson().toJson(in, Map.class));
    }

}

class InjectedClassLoader extends ClassLoader {

    public Map<String, byte[]> injections = new HashMap<>();

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if(injections.containsKey(name)) {
            byte[] clazz = injections.get(name);
            return defineClass(name, clazz, 0, clazz.length);
        } else {
            return super.findClass(name);
        }
    }
}