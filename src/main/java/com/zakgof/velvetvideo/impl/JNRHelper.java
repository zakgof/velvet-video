package com.zakgof.velvetvideo.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zakgof.velvetvideo.VelvetVideoException;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Platform;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.byref.PointerByReference;
import jnr.ffi.provider.ParameterFlags;

public class JNRHelper {

	private static final String MIN_NATIVE_VERSION = "0.2.7";

	private static final Logger LOG = LoggerFactory.getLogger("velvet-video");
    private static final String PLATFORM = getPlatform();
    private static final File extractionDir = initializeExtractionDirectory();

    private static final Map<Class<?>, Object> libCache = new HashMap<>();

    private static File initializeExtractionDirectory() {
    	String path = "velvet-video-natives/version.inf";
		URL resource = Thread.currentThread().getContextClassLoader().getResource(path);
    	if (resource == null) {
    		throw new VelvetVideoException("Cannot locate native libs. Make sure that velvet-video-natives in on classpath.");
    	}
    	try {
	    	URLConnection connection = resource.openConnection();
	    	connection.connect();
	    	try (InputStream is = connection.getInputStream()) {
	        	Properties props = new Properties();
	        	props.load(is);
	        	String version = props.getProperty("Version");
	        	LOG.info("Loading velvet-video-natives version " + version);
	        	if (version.compareTo(MIN_NATIVE_VERSION) < 0) {
	        		throw new VelvetVideoException("Minimum compatible version of velvet-video-natives is " + MIN_NATIVE_VERSION + ", detected version " + version);
	        	}
	        	return createExtractionDirectory(version);
	    	}
        } catch (IOException e) {
			throw new VelvetVideoException("Error while loading native version manifest", e);
		}
	}

	private static File createExtractionDirectory(String version) {
		String home = System.getProperty("user.home");
		File dir = Paths.get(home, ".velvet-video", "natives", version).toFile();
		if (!dir.exists() && !dir.mkdirs()) {
			throw new VelvetVideoException("Cannot create a dir for extracting native libraries.");
		}
		LOG.atDebug().addArgument(dir).log("Velvet-video native extraction location is {}");
		return dir;
	}

	@SuppressWarnings("unchecked")
	public static <L> L load(Class<L> clazz, String libShortName, int libVersion) {
		return clazz.cast(libCache.computeIfAbsent(clazz, (Class<?> cl) -> JNRHelper.forceLoad((Class<L>)cl, libShortName, libVersion)));
	}

	private static <L> L forceLoad(Class<L> clazz, String libShortName, int libVersion) {
		String libPath = null;
        try {
        	LOG.atDebug().addArgument(libShortName).addArgument(libVersion).log("Requesting loading native lib {}.{}");
        	Platform nativePlatform = Platform.getNativePlatform();
            String libfile = libVersionAndName(libShortName, libVersion);
            libShortName = fixShortName(libShortName, libVersion);

            libPath = nativePlatform.locateLibrary(libShortName, Arrays.asList(extractionDir.toString()));
            LOG.atDebug().addArgument(libfile).addArgument(extractionDir).log("Checking native lib {} at {}");
            if (!new File(libPath).isAbsolute()) {
            	extractNatives();
            }
            L lib = LibraryLoader.create(clazz).failImmediately().search(extractionDir.toString()).load(libShortName);
            LOG.atDebug().addArgument(libfile).log("Loaded {}");
			return lib;
        } catch(UnsatisfiedLinkError e) {
        	LOG.error("Error loading native library " + libPath, e);
            throw new VelvetVideoException("Error loading native library " + libPath, e);
        }
    }

	private static void extractNatives() {
		LOG.atInfo().log("Extracting native libraries");
		String folder = "velvet-video-natives/" + PLATFORM + "/";
		URL resource = Thread.currentThread().getContextClassLoader().getResource(folder);
		LOG.atDebug().addArgument(folder).addArgument(resource).log("Resolving classpath {} --> {}");
		if (resource == null) {
			throw new VelvetVideoException("Cannot locate native libraries resource " + folder + ". Make sure that velvet-video-natives in on classpath.");
		}
		File location = locationFor(resource);
		boolean isJar = location.isFile();
		if (isJar) {
		    try (ZipFile zif = new ZipFile(location)) {
		        zif.stream().filter(zipEntry -> !zipEntry.isDirectory() && zipEntry.getName().startsWith(folder))
		            .forEach(zipEntry -> {
		                String rawfilename = zipEntry.getName().split("/")[2];
		                File destFile = new File(extractionDir, rawfilename);
		                LOG.atInfo().addArgument(zipEntry).addArgument(destFile.toString())
		                	.log("Extracting {} --> {}");
		                try (InputStream inputStream = zif.getInputStream(zipEntry)) {
		                    FileUtils.copyInputStreamToFile(inputStream, destFile);
		                } catch (IOException e) {
		                    throw new VelvetVideoException("Error extracting ffmpeg native libraries", e);
		                }
		            });
		    } catch (IOException e) {
		        throw new VelvetVideoException("Error extracting ffmpeg native libraries", e);
		    }
		} else {
			throw new VelvetVideoException("Loading velvet-video-natives from jar only is supported");
		}
	}

	private static String fixShortName(String libShortName, int libVersion) {
		if (PLATFORM.startsWith("windows")) {
			return libShortName + "-" + libVersion;
		}
		return libShortName;
	}

	private static String libVersionAndName(String libName, int libVersion) {
		if (PLATFORM.startsWith("linux")) {
			return "lib" + libName + ".so." + libVersion;
		} else if (PLATFORM.startsWith("windows")) {
			return libName + "-" + libVersion + ".dll";
		}
		throw new VelvetVideoException("Unsupported platform: " + PLATFORM);
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

    public static <T extends Struct> T struct(Class<T> clazz, Pointer value) {
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

	public static <T extends Struct> T struct(Class<T> clazz, PointerByReference pp) {
		return struct(clazz, pp.getValue());
	}

    public static Pointer ptr(Struct.NumberField member) {
        return member.getMemory().slice(member.offset());
    }

	// TODO
	public static int preload(String libShortName, int libVersion) {
		File[] files = extractionDir.listFiles();
		if (files == null || files.length == 0) {
			extractNatives();
		}
		String libfile = libVersionAndName(libShortName, libVersion);
		File nativeLib = new File(extractionDir, libfile);
		String nativeLibPath = nativeLib.toString();
		if (!nativeLib.exists()) {
			LOG.atDebug().addArgument(nativeLibPath).log("Skipping non-existing native lib dependendcy file: {}");
			return -1;
		}
		LOG.atDebug().addArgument(nativeLibPath).log("Preloading native lib dependendcy: {}");
		try {
			System.load(nativeLibPath);
		} catch (LinkageError e) {
			LOG.atError().addArgument(nativeLibPath).addArgument(e.getMessage())
					.log("Error preloading native lib dependency {} : {}");
		}
		return 0;
	}
}
