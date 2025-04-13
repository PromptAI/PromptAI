package com.zervice.kbase.utils;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

@Log4j2
public class ReflectionUtils {
    /**
     * The spring way ...
     *
     * @param packageName
     * @return
     * @throws IOException
     */
    public static Class[] getClasses(String packageName) throws IOException, ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(true);

        scanner.addIncludeFilter(new TypeFilter() {
            @Override
            public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
                return true;
                /*
                ClassMetadata classMetadata = metadataReader.getClassMetadata();
                if(classMetadata.getClassName().contains(packageName)) {
                    LazyLog.info("Found - " + classMetadata.getClassName());
                    return true;
                }
                else {
                    LazyLog.info("Ignore - " + classMetadata.getClassName());
                    return false;
                }
                */
            }
        });

        Set<BeanDefinition> beans = scanner.findCandidateComponents(packageName);

        ArrayList<Class> classes = new ArrayList<>();
        for(BeanDefinition bean : beans) {
            classes.add(Class.forName(bean.getBeanClassName()));
        }

        // new Reflections(packageName).getAllTypes().stream().forEach(name -> LazyLog.info("Found - " + name));
        //.getTypesAnnotatedWith(MyAnnotation.class)

        return classes.toArray(new Class[0]);
    };

    /**
     * Not working in spring case ... as Spring try to put classes in one JAR
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     */
    public static Class[] getClasses2(String packageName)
            throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration resources = classLoader.getResources(path);

        LOG.info("Try load resources from path " + path);
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = (URL) resources.nextElement();
            LOG.info("Add resource dir - " + resource.toString());
            dirs.add(new File(resource.getFile()));
        }
        LOG.info("Found directory count - " + dirs.size());

        ArrayList<Class> classes = new ArrayList();
        for (File directory : dirs) {
            LOG.info("Search resource dir - " + directory.getPath());
            classes.addAll(findClasses(directory, packageName));
        }

        LOG.info("Found total classes count - " + classes.size());
        return classes.toArray(new Class[classes.size()]);
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirs.
     */
    public static List findClasses(File directory, String packageName) throws ClassNotFoundException {
        List classes = new ArrayList();
        if (!directory.exists()) {
            LOG.info("Directory not existing " + directory.getPath());

            File file = new File("file:/home/ning/work/zp/backend/build/libs/backend.jar!/BOOT-INF/classes/com/zoomphant/database/dao");
            LOG.info("File exists? " + file.exists());

            // return classes;
        }

        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                LOG.info("Try load subdir - " + file.getPath());
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                LOG.info("Try add class file - " + file.getPath());
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
            else {
                LOG.info("Ignore file - " + file.getPath());
            }
        }
        return classes;
    }

}
