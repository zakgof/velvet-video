package com.zakgof.velvetvideo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Platform;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.provider.ParameterFlags;

class JNRHelper {


	private static Logger LOG = LoggerFactory.getLogger("velvet-video");
    private static String PLATFORM = getPlatform();
    private static File extractionDir = createExtractionDirectory();

	private static File createExtractionDirectory() {
		String home = System.getProperty("user.home");
		File dir = Paths.get(home, ".velvet-video", "natives", "0.0.0").toFile(); // TODO
		if (!dir.exists() && !dir.mkdirs()) {
			throw new VelvetVideoException("Cannot create a dir for extracting native libraries.");
		}
		return dir;
	}

    static <L> L load(Class<L> clazz, String libName) {

        try {

        	Platform nativePlatform = Platform.getNativePlatform();
            String libfile = nativePlatform.mapLibraryName(libName);
        	String folder = "binaries/" + PLATFORM + "/";
			String path = folder + libfile;
			URL resource = Thread.currentThread().getContextClassLoader().getResource(path);
        	if (resource == null) {
        		throw new VelvetVideoException("Cannot locate native library " + libfile + ". Make sure that velvet-video-natives in on classpath.");
        	}
			File location = locationFor(resource);
            boolean isJar = location.isFile();
            String libPath = nativePlatform.locateLibrary(libName, Arrays.asList(extractionDir.toString()));
            if (libPath.equals(libfile) && isJar) {
                try (ZipFile zif = new ZipFile(location)) {
                    zif.stream().filter(zipEntry -> !zipEntry.isDirectory() && zipEntry.getName().startsWith(folder))
                        .forEach(zipEntry -> {
                            String rawfilename = zipEntry.getName().split("/")[2];
                            LOG.info("Extracting " + zipEntry + " to " + new File(extractionDir, rawfilename));
                            try (InputStream inputStream = zif.getInputStream(zipEntry)) {
                                FileUtils.copyInputStreamToFile(inputStream, new File(extractionDir, rawfilename));
                            } catch (IOException e) {
                                throw new VelvetVideoException("Error extracting ffmpeg native libraries");
                            }
                        });
                }  catch (IOException e) {
                    throw new VelvetVideoException("Error extracting ffmpeg native libraries");
                }
            }
            L lib = LibraryLoader.create(clazz).search(extractionDir.toString()).load(libName);
			return lib;
        } catch(UnsatisfiedLinkError e) {
            throw new VelvetVideoException("Error loading native libraries. Make sure that velvet-video-natives in on classpath");
        }
    }

	private static File locationFor(URL url) {
		try {
			if (url.toString().startsWith("jar:file:")) {

				JarURLConnection connection = (JarURLConnection) url.openConnection();
				final URL jarurl = connection.getJarFileURL();
				String zipFile = jarurl.getFile();
				return new File(zipFile);
			}
			return new File(url.toURI());
		} catch (URISyntaxException | IOException e) {
			throw new VelvetVideoException("Error locating ffmpeg native libraries: " + url);
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
