package com.webobjects.foundation.development;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.foundation.NSLog;
import com.webobjects.foundation.NSNestedProperties;
import com.webobjects.foundation.NSPropertyListSerialization;

public class CFBundleAdaptor implements NSBundleAdaptorProvider {

	@Override
	public String adaptorBundlePath(final FileSystem fs, final String bundlePath) {
		return rootForBundlePath(fs, bundlePath).toString();
	}

	@Override
	public NSBundleInfo bundleInfoFromFileSystem(final FileSystem fs, final Path fsBundlePath) {
		try {
			final Path infoPath = fsBundlePath.resolve("Contents/Info.plist");
			final String infoString = Files.readAllLines(infoPath).stream()
					.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();
			final NSDictionary dict = NSPropertyListSerialization.dictionaryForString(infoString);
			return NSBundleInfo.forDictionary(dict);
		} catch (final IOException e) {
			throw NSForwardException._runtimeExceptionForThrowable(e);
		}
	}

	@Override
	public Stream<String> classNamesForFileSystem(final FileSystem fs, final Path fsBundlePath) {
		try {
			final Path walkPath = fsBundlePath.resolve("Contents/Resources/Java/");
			return Files.walk(walkPath)
					// only files
					.filter(Files::isRegularFile)
					// that are .classes
					.filter(path -> path.getFileName().toString().endsWith(".class")).map(Path::toString)
					// trim off .class extension, relativePath, and leading /
					.map(name -> name.substring(walkPath.toString().length() + 1, name.length() - ".class".length()))
					// replace path separators with .
					.map(name -> name.replace('/', '.'))
					// return the interned string
					.map(String::intern);
		} catch (final IOException e) {
			throw NSForwardException._runtimeExceptionForThrowable(e);
		}
	}

	@Override
	public Path fsBundlePath(final FileSystem fs, final String bundlePath) {
		return fs.getPath(bundlePath);
	}

	@Override
	public boolean isAdaptable(final FileSystem fs, final String bundlePath) {
//		final Path infoPath = relativePath.resolve("Contents/Info.plist");
//		return Files.exists(infoPath) && Files.isReadable(infoPath);
		return rootForBundlePath(fs, bundlePath) != null;
	}

	@Override
	public Properties propertiesForFileSystem(final FileSystem fs, final Path fsBundlePath) {
		final Path propsPath = fsBundlePath.resolve("Contents/Resources/Properties");
		final NSNestedProperties nested = new NSNestedProperties(null);
		try (InputStream propertiesStream = Files.newInputStream(propsPath)) {
			nested.load(propertiesStream);
		} catch (final NoSuchFileException e) {
			if (NSLog._debugLoggingAllowedForLevelAndGroups(NSLog.DebugLevelDetailed,
					NSLog.DebugGroupResources | NSLog.DebugGroupWebObjects)) {
				NSLog.debug.appendln(e.getMessage());
			}
		} catch (final IOException e) {
			throw NSForwardException._runtimeExceptionForThrowable(e);
		}
		return nested;
	}

	@Override
	public List<Path> resourcePathsForFileSystem(final FileSystem fs, final Path fsBundlePath) {
		return Arrays.asList(fsBundlePath.resolve("Contents/Resources"),
				fsBundlePath.resolve("Contents/WebServerResources"));
	}

	private Path rootForBundlePath(final FileSystem fs, final String bundlePath) {
		final Path infoPath = fs.getPath("Contents/Info.plist");
		for (Path p = fs.getPath(bundlePath); p.getParent() != null; p = p.getParent()) {
			final Path resolved = p.resolve(infoPath);
			if (Files.exists(resolved) && Files.isReadable(resolved)) {
				return p;
			}
			final Path buildPath = p.resolve(fs.getPath("build"));
			if (Files.exists(buildPath)) {
				try (Stream<Path> stream = Files.list(buildPath)) {
					final Optional<Path> found = stream.filter(Files::isDirectory).findFirst().filter(bp -> {
						final Path bpInfo = bp.resolve(infoPath);
						return Files.exists(bpInfo) && Files.isReadable(bpInfo);
					});
					if (found.isPresent()) {
						return found.get();
					}
				} catch (final IOException e) {
					throw NSForwardException._runtimeExceptionForThrowable(e);
				}
			}
		}
		return null;
	}

}
