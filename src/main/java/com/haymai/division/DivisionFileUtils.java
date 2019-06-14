package com.haymai.division;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Description:
 *
 * @author haymai
 * @version 1.0
 * @date 6/13/2019 8:25 PM
 * @since JDK 1.8
 */
public class DivisionFileUtils {

	private DivisionFileUtils() {
	}

	private static final Logger LOG = LoggerFactory.getLogger(DivisionFileUtils.class);

	private static final String DEFAULT_FILE_PATH = "/division/2019.txt";
	private static final String PARENT_PATH = "/app/division/";

	private static final String FILE_PATH = String.format("%s/%%s.txt", PARENT_PATH);

	public static boolean isFileExists(String fileCode) {
		File file = new File(String.format(FILE_PATH, fileCode));
		return file.exists();
	}

	public static void saveFile(String fileCode, List<String> result) {
		try {
			FileUtils.writeLines(new File(String.format(FILE_PATH, fileCode)), result);
		} catch (IOException e) {
			LOG.error("save division result error !!!", e);
		}
	}

	public static InputStream getInputStream() {
		File parentFilePath = new File(PARENT_PATH);
		File[] listFiles = parentFilePath.listFiles();
		if (listFiles == null || listFiles.length == 0) {
			return DivisionFileUtils.class.getResourceAsStream(DEFAULT_FILE_PATH);
		}
		List<File> files = Arrays.asList(listFiles);
		files.sort((o1, o2) -> {
			if (o1.isDirectory() && o2.isFile()) {
				return -1;
			}
			if (o1.isFile() && o2.isDirectory()) {
				return 1;
			}
			return o2.getName().compareTo(o1.getName());
		});
		try {
			return FileUtils.openInputStream(files.get(0));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
