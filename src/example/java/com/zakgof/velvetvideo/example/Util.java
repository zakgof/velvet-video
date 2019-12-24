package com.zakgof.velvetvideo.example;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Util {
	public static File getFile(String url, String localname) {
		String home = System.getProperty("user.home");
		File file = Paths.get(home, ".velvet-video", "examples", localname).toFile();
		file.getParentFile().mkdirs();
		if (!file.exists()) {
			try (InputStream in = new URL(url).openStream()) {
				Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return file;
 	}

	public static File workDir() {
		String home = System.getProperty("user.home");
		File dir = Paths.get(home, ".velvet-video", "examples").toFile();
		dir.mkdirs();
		return dir;
 	}
}
