package ai.arcblroth.taterwebz.util;

import net.fabricmc.loader.transformer.accesswidener.AccessWidener;
import net.fabricmc.loader.transformer.accesswidener.AccessWidenerVisitor;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.function.Consumer;

public class NotKnotClassLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();
    }

    private final MethodHandle findLoadedClass;
    private final AccessWidener accessWidener;
    private HashMap<String, Consumer<ClassNode>> transformers;

    public NotKnotClassLoader(ClassLoader parent) {
        this(parent, null);
    }

    public NotKnotClassLoader(ClassLoader parent, AccessWidener accessWidener) {
        super(new URL[0], parent);
        transformers = new HashMap<>();

        try {
            Method findLoadedClassMethod = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            findLoadedClassMethod.setAccessible(true);
            findLoadedClass = MethodHandles.lookup().unreflect(findLoadedClassMethod);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        this.accessWidener = accessWidener;
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    public void addClassTransformer(String clazz, Consumer<ClassNode> transformer) {
        this.transformers.put(clazz, transformer);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                try {
                    if(findLoadedClass.invoke(getParent(), name) == null) {
                        try {
                            c = findClass(name);
                        } catch (ClassNotFoundException e) {
                            // if not found
                        }
                    }
                } catch (IllegalAccessException | InvocationTargetException ignored) {

                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }

                if (c == null) {
                    return super.loadClass(name, resolve);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/').concat(".class");
        if(name.startsWith("javax.media") || (!name.startsWith("java") && !name.startsWith("sun") && !name.startsWith("jdk"))) {
            InputStream stream = super.getResourceAsStream(path);
            if (stream != null) {
                try {
                    byte[] clazzSource = IOUtils.toByteArray(stream);
                    if(accessWidener != null || transformers.containsKey(name)) {
                        ClassReader reader = new ClassReader(clazzSource);
                        ClassNode node = new ClassNode();
                        reader.accept(node, 0);
                        if (transformers.containsKey(name)) {
                            transformers.get(name).accept(node);
                        }
                        ClassWriter writer = new ClassWriter(0);
                        if(accessWidener != null) {
                            node.accept(new AccessWidenerVisitor(Opcodes.ASM8, writer, accessWidener));
                        } else {
                            node.accept(writer);
                        }
                        clazzSource = writer.toByteArray();
                    }
                    try {
                        stream.close();
                    } catch (IOException ignored) {}
                    return defineClass(name, clazzSource, 0, clazzSource.length);
                } catch (IOException ignored) {
                    throw new ClassNotFoundException("Force loaded class " + name + " not found!");
                }
            }
        }
        return null;
    }

}
