package ai.arcblroth.taterwebz.util;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

public class NotKnotClassLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();
    }

    private final Method findLoadedClass;
    private HashMap<String, Consumer<ClassNode>> transformers;

    public NotKnotClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
        transformers = new HashMap<>();

        try {
            findLoadedClass = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            findLoadedClass.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
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
                } catch (IllegalAccessException | InvocationTargetException ignored) {}

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
        if(!name.startsWith("java.") && !name.startsWith("sun.")) {
            InputStream stream = super.getResourceAsStream(path);
            if (stream != null) {
                try {
                    byte[] clazzSource = IOUtils.toByteArray(stream);
                    if(transformers.containsKey(name)) {
                        ClassReader reader = new ClassReader(clazzSource);
                        ClassNode node = new ClassNode();
                        reader.accept(node, 0);
                        transformers.get(name).accept(node);
                        ClassWriter writer = new ClassWriter(0);
                        node.accept(writer);
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
