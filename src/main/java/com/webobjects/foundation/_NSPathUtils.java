package com.webobjects.foundation;

import java.net.URL;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public final class _NSPathUtils {
	public static final Function<Path, String> FILE_NAME = path -> path.getFileName().toString();

	public static final Function<String, Function<Path, String>> FILE_NAME_TRIMMED = sep -> path -> {
		String fileName = path.getFileName().toString();
		if (fileName.endsWith(sep)) {
			/*
			 * ZipFileSystem has a trailing slash on directories, but UnixFileSystem does
			 * not.
			 */
			// Trim off the trailing path name separator
			fileName = fileName.substring(0, fileName.length() - sep.length());
		}
		return fileName;
	};

	public static final Function<Path, String> PATH_TO_STRING = Path::toString;

	public static final Function<String, Function<Path, String>> PATH_TO_STRING_TRIMMED = sep -> path -> {
		String pathString = path.toString();
		if (pathString.endsWith(sep)) {
			pathString = pathString.substring(0, pathString.length() - sep.length());
		}
		return pathString;
	};

	public static final Function<Function<Path, String>, Function<String, Predicate<Path>>> IS_EXTENSION = fileNameMapper -> ext -> path -> fileNameMapper
			.apply(path).endsWith(normalizeExtension(ext));

	public static final Function<Function<Path, String>, Function<String, Predicate<Path>>> IS_NAMED = fileNameMapper -> name -> path -> {
		final String fileName = fileNameMapper.apply(path);
		return name.equals(fileName);
	};

	public static final String LPROJSUFFIX = ".lproj";

	public static final String NONLOCALIZED_LPROJ = "Nonlocalized.lproj";

	public static void loadPathResultForLocale(final ConcurrentHashMap<Path, String> results, final Locale locale,
			final Path basePath, final Path pathToLoad) {
		final Path relativePath = pathToLoad.subpath(basePath.getNameCount(), pathToLoad.getNameCount());
		final String head = relativePath.subpath(0, 1).toString();
		final String lproj = locale.getDisplayLanguage(Locale.ENGLISH) + LPROJSUFFIX;
		if (head.equals(lproj)) {
			final Path tail = relativePath.subpath(1, relativePath.getNameCount());
			results.put(tail, head);
		} else if (!head.endsWith(LPROJSUFFIX)) {
			results.putIfAbsent(relativePath, NONLOCALIZED_LPROJ);
		}
	}

	public static Path nonLocalizePath(final Path basePath, final Path pathToAppend) {
		final Path relativePath = pathToAppend.subpath(basePath.getNameCount(), pathToAppend.getNameCount());
		final String head = relativePath.subpath(0, 1).toString();
		final Path result;
		if (head.endsWith(LPROJSUFFIX)) {
			result = relativePath;
		} else {
			result = basePath.getFileSystem().getPath(NONLOCALIZED_LPROJ).resolve(relativePath);
		}
		return result;
	}

	private static String normalizeExtension(final String extension) {
		return extension == null || extension.isEmpty()
				// If null, all paths will end with empty string.
				? ""
				// Otherwise, does it start with a dot?
				: extension.charAt(0) == '.'
						// Yes? It's good.
						? extension
						// No? Add the dot.
						: "." + extension;
	}

	public static String originalResourcePath(final String aResourcePath) {
		final String path = Optional.of(aResourcePath).filter(p -> p.startsWith(NONLOCALIZED_LPROJ))
				.map(p -> p.substring(NONLOCALIZED_LPROJ.length())).orElse(aResourcePath);
		return path.startsWith("/") ? path.substring(1) : path;
	}

	public static String pathFromJarFileUrl(final URL url) {
		if ("jar".equals(url.getProtocol())) {
			final String path = url.getPath();
			final int index = path.lastIndexOf("!/");
			final String pathToJar = index == -1 ? path : path.substring(0, index);
			return NSValueUtilities.urlValue(pathToJar).filter(u -> "file".equals(u.getProtocol())).map(URL::getPath)
					.orElseThrow(() -> new IllegalArgumentException("URL is not a jar:file: url " + url.toString()));
		}
		throw new IllegalArgumentException("URL is not a jar:file: url " + url.toString());
	}

	public static NSArray<String> unloadPathResults(final ConcurrentHashMap<Path, String> results,
			final Function<Path, String> pathToString) {
		return NSValueUtilities.mutableArrayFromStream(results.entrySet().stream()
				.map(entry -> entry.getKey().getFileSystem().getPath(entry.getValue()).resolve(entry.getKey()))
				.map(pathToString).sorted()).immutableClone();
	}

	private _NSPathUtils() {
	}
}
