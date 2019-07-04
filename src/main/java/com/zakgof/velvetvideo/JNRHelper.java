package com.zakgof.velvetvideo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Platform;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.provider.ParameterFlags;

class JNRHelper {

    private static String PLATFORM = getPlatform();

    static <L> L load(Class<L> clazz, String libName) {
        
        try {
            URI uri = JNRHelper.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            File location = new File(uri);
            boolean isJar = location.isFile();
            File dir = isJar ? location.getParentFile() : new File(location, PLATFORM);
            Platform nativePlatform = Platform.getNativePlatform();
            String libfile = nativePlatform.mapLibraryName(libName);
            String libPath = nativePlatform.locateLibrary(libName, Arrays.asList(dir.toString()));
            if (libPath.equals(libfile) && isJar) {
                try (ZipFile zif = new ZipFile(location)) {
                    zif.stream().filter(zipEntry -> !zipEntry.isDirectory() && zipEntry.getName().startsWith(PLATFORM))
                        .forEach(zipEntry -> {
                            String rawfilename = zipEntry.getName().split("/")[1];
                            System.err.println("Extracting " + zipEntry + " to " + new File(dir, rawfilename));
                            try (InputStream inputStream = zif.getInputStream(zipEntry)) {
                                FileUtils.copyInputStreamToFile(inputStream, new File(dir, rawfilename));
                            } catch (IOException e) {
                                throw new VelvetVideoException("Error extracting ffmpeg native libraries");
                            }
                        });
                }  catch (IOException e) {
                    throw new VelvetVideoException("Error extracting ffmpeg native libraries");
                }
            }
            return LibraryLoader.create(clazz).search(dir.toString()).load(libName);
        } catch(URISyntaxException e) {
            throw new VelvetVideoException("Error locating ffmpeg native libraries");
        }
    }

    private static String getPlatform() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        if (os.contains("windows") && arch.contains("64")) {          
            return "windows64";
        } else if (os.contains("linux") && arch.contains("64")) {
            return "linux64";
        } else {
            throw new VelvetVideoException("Unsupported platform. Supported platforms are windows64 and linux64");
        }
    }

    static <T extends Struct> T struct(Class<T> clazz, Pointer value) {
        try {
            Constructor<T> constructor = clazz.getConstructor(Runtime.class);
            T instance = constructor.newInstance(value.getRuntime());
            instance.useMemory(value);
            T.getMemory(instance, ParameterFlags.OUT);
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new VelvetVideoException(e);
        }
    }

    public static Pointer ptr(Struct.NumberField member) {
        return member.getMemory().slice(member.offset());
    }
}
