package com.webobjects.foundation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.webobjects.foundation.development.NSBundleAdaptorProvider;
import com.webobjects.foundation.development.NSBundleInfo;
import com.webobjects.foundation.development.NSBundleInfo.CFBundlePackageType;

public class NSBundle {
	public static final String CFBUNDLESHORTVERSIONSTRINGKEY = NSBundleInfo.CF_BUNDLE_SHORT_VERSION_STRING_KEY;
	public static final String MANIFESTIMPLEMENTATIONVERSIONKEY = NSBundleInfo.MANIFEST_IMPLEMENTATION_VERSION_KEY;
	public static final String NS_GLOBAL_PROPERTIES_PATH = "NSGlobalPropertiesPath";
	private static final String LEGACY_GLOBAL_PROPERTIES_PATH = "WebObjectsPropertiesReplacement";

	public static final String AllBundlesDidLoadNotification = "NSBundleAllDidLoadNotification";
	public static final String BundleDidLoadNotification = "NSBundleDidLoadNotification";
	public static final String LoadedClassesNotification = "NSLoadedClassesNotification";
	private static final ConcurrentHashMap<String, NSBundle> BUNDLES_BY_CLASS_NAME = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, NSBundle> BUNDLES_BY_NAME = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, NSBundle> BUNDLES_BY_PATH = new ConcurrentHashMap<>();
	private static final _NSThreadsafeMutableArray<NSBundle> FRAMEWORK_BUNDLES = new _NSThreadsafeMutableArray<>(
			new NSMutableArray<>());
	private static NSBundle mainBundle;

	static {
		// read NSBundles from jars
		try {
			final String searchPath = "Resources/Info.plist";
			final Enumeration<URL> e1 = NSBundle.class.getClassLoader().getResources(searchPath);
			final Enumeration<URL> e2 = ClassLoader.getSystemResources(searchPath);
			final Stream<URL> stream = Stream.concat(Collections.list(e1).stream(), Collections.list(e2).stream());
			stream.map(_NSPathUtils::pathFromJarFileUrl).distinct().forEach(NSBundle::loadBundleWithPath);

			// read bundles from classpath
			final String classPath = System.getProperty("java.class.path");
			final List<String> paths = Arrays.asList(classPath.split(File.pathSeparator));
			Optional.ofNullable(NSProperties.getProperty("com.webobjects.classpath"))
					.map(wcp -> Arrays.asList(wcp.split(File.pathSeparator))).ifPresent(wcp -> paths.addAll(wcp));
			// skip any jar bundles which are already loaded and check everything else
			final List<String> remainingPaths = paths.stream().filter(p -> !BUNDLES_BY_PATH.containsKey(p)).distinct()
					.collect(Collectors.toList());
			remainingPaths.forEach(NSBundle::loadBundleWithPath);

			loadBundleProperties();
			NSNotificationCenter.defaultCenter().postNotification(AllBundlesDidLoadNotification, null, null);
			_NSUtilities._setResourceSearcher(new _NSUtilities._ResourceSearcher() {
				@Override
				public Class _searchForClassWithName(final String className) {
					if (NSLog._debugLoggingAllowedForLevelAndGroups(NSLog.DebugLevelInformational,
							NSLog.DebugGroupResources)) {
						NSLog.debug.appendln("searchForClassWithName(\"" + className
								+ "\") was invoked for all bundles.\n\t**This affects performance very badly.**");
						if (NSLog.debug.allowedDebugLevel() > NSLog.DebugLevelInformational) {
							NSLog.debug.appendln(
									new RuntimeException("NSBundle.searchAllBundlesForClassWithName was invoked."));
						}
					}
					return _allBundlesReally().stream()
							.map(b -> Optional.ofNullable(_NSUtilities._searchForClassInPackages(className,
									b.bundleClassPackageNames(), true, false)))
							.filter(Optional::isPresent).map(Optional::get).findFirst().orElse(null);
				}

				@Override
				public URL _searchPathURLForResourceWithName(final Class resourceClass, final String resourceName,
						final String extension) {
					final URL url = null;
					final NSBundle bundle = NSBundle.bundleForClass(resourceClass);
					if (bundle != null && resourceName != null) {
						String fileName = null;
						if (extension == null || extension.length() == 0) {
							fileName = resourceName;
						} else if (extension.startsWith(".") || resourceName.endsWith(".")) {
							fileName = resourceName + extension;
						} else {
							fileName = resourceName + "." + extension;
						}
						bundle.pathURLForResourcePath(bundle.resourcePathForLocalizedResourceNamed(fileName, ""));
					}
					return url;
				}
			});
		} catch (final IOException e) {
			throw NSForwardException._runtimeExceptionForThrowable(e);
		}
	}

	public static NSArray<NSBundle> _allBundlesReally() {
		return Optional.ofNullable(mainBundle())
				.map(b -> new NSArray<NSBundle>(mainBundle()).arrayByAddingObjectsFromArray(frameworkBundles()))
				.orElse(frameworkBundles());
	}

	public static synchronized NSBundle _appBundleForName(final String aName) {
		return Optional.ofNullable(aName)
				.map(name -> name.endsWith(".woa") ? NSPathUtilities.stringByDeletingPathExtension(name) : name)
				.map(name -> name.equals(mainBundle().name())).map(name -> mainBundle()).orElse(null);
	}

	public static void _setMainBundle(final NSBundle bundle) {
		// TODO remove this?
		throw new UnsupportedOperationException("Not implemented");
	}

	@Deprecated
	public static synchronized NSArray<NSBundle> allBundles() {
		return new NSArray<>(mainBundle());
	}

	@Deprecated
	public static NSArray<NSBundle> allFrameworks() {
		return frameworkBundles();
	}

	public static NSBundle bundleForClass(final Class<?> aClass) {
		return aClass == null ? null : BUNDLES_BY_CLASS_NAME.get(aClass.getName());
	}

	public static NSBundle bundleForName(final String aName) {
		return aName == null ? null : BUNDLES_BY_NAME.get(aName);
	}

	public static NSBundle bundleWithPath(final String aPath) {
		return aPath == null ? null : BUNDLES_BY_PATH.get(aPath);
	}

	private static FileSystem fileSystemForBundlePath(final String bundlePath) {
		if (!bundlePath.endsWith(".jar")) {
			return FileSystems.getDefault();
		}
		final URI uri = uriForBundlePath(bundlePath);
		try {
			return "jar".equals(uri.getScheme()) ? FileSystems.newFileSystem(uri, Collections.emptyMap())
					: FileSystems.getDefault();
		} catch (final IOException e) {
			throw NSForwardException._runtimeExceptionForThrowable(e);
		}
	}

	public static synchronized NSArray<NSBundle> frameworkBundles() {
		return FRAMEWORK_BUNDLES.immutableClone();
	}

	private static Optional<File> globalPropertiesFile() {
		final Optional<File> homeDir = Optional.ofNullable(System.getProperty("user.home"))
				.filter(path -> path.length() > 0).map(File::new).filter(File::isDirectory);
		final File propsFile = NSValueUtilities
				// get props file path
				.coalesce(() -> System.getProperty(NS_GLOBAL_PROPERTIES_PATH),
						() -> System.getProperty(LEGACY_GLOBAL_PROPERTIES_PATH))
				// new file if path is not null or empty
				.filter(path -> path.length() > 0).map(File::new)
				// return file if absolute, otherwise assume it is in the user.home
				.map(file -> homeDir.map(dir -> file.isAbsolute() ? file : new File(dir, file.getPath())).orElse(file))
				// otherwise, just look for webobjects properties in the home dir
				.orElseGet(() -> homeDir.map(dir -> new File(dir, "WebObjects.properties"))
						// Make sure it exists
						.filter(file -> file.exists() && file.isFile() && file.canRead()).orElse(null));
		return Optional.ofNullable(propsFile);

	}

	private static Properties loadBundleGlobalProperties(final File propsFile, final Properties bundleProps) {
		try {
			final NSNestedProperties nested = new NSNestedProperties(null);
			nested.putAll(bundleProps);
			nested.load(propsFile);
			return nested;
		} catch (final Exception e) {
			throw new RuntimeException("Failed to load '" + propsFile + "'.", e);
		}
	}

	private static void loadBundleProperties() {
		final Properties bundleProps = frameworkBundles().stream().map(NSBundle::properties).collect(Properties::new,
				Properties::putAll, Properties::putAll);
		Optional.ofNullable(mainBundle()).ifPresent(b -> bundleProps.putAll(b.properties()));
		final Properties userProps = globalPropertiesFile()
				.map(propsFile -> loadBundleGlobalProperties(propsFile, bundleProps)).orElse(bundleProps);
		final Properties sysProps = new Properties();
		sysProps.putAll(userProps);
		Optional.ofNullable(NSProperties._getProperties()).ifPresent(sysProps::putAll);
		NSProperties._setProperties(sysProps);
	}

	private static NSBundle loadBundleWithPath(final String bundlePath) {
		final FileSystem bundleFs = fileSystemForBundlePath(bundlePath);
		final ServiceLoader<NSBundleAdaptorProvider> loader = ServiceLoader.load(NSBundleAdaptorProvider.class);
		final NSBundle bundle = StreamSupport.stream(loader.spliterator(), false)
				.filter(adaptor -> adaptor.isAdaptable(bundleFs, bundlePath)).findFirst()
				.map(adaptor -> new NSBundle(bundleFs, bundlePath, adaptor)).orElse(null);
		if (bundle != null) {
			BUNDLES_BY_NAME.compute(bundle.name(), (key, value) -> {
				if (value == null) {
					NSLog.out.appendln("Bundle loaded with name " + bundle.name() + " and adaptor "
							+ bundle.adaptor.getClass().getName());
					if (bundle.isFramework()) {
						FRAMEWORK_BUNDLES.addObject(bundle);
					} else {
						// There can be only one
						mainBundle = bundle;
					}
					BUNDLES_BY_PATH.put(bundle.bundlePath(), bundle);
					// load principal class and post notification
					bundle.principalClass();
					bundle.postNotification();
					return bundle;
				}
				if (NSLog._debugLoggingAllowedForLevelAndGroups(NSLog.DebugLevelDetailed,
						NSLog.DebugGroupResources | NSLog.DebugGroupWebObjects)) {
					NSLog.debug.appendln("Bundle with name " + key + " already loaded. Discarding.");
				}
				return value;
			});
		}
		return bundle;
	}

	public static NSBundle mainBundle() {
		if (mainBundle == null) {
			final String mainBundleName = NSProperties._mainBundleName();
			final String userDir = Optional.ofNullable(NSProperties.getProperty("webobjects.user.dir"))
					.orElse(System.getProperty("user.dir"));
			mainBundle = NSValueUtilities.coalesce(() -> bundleForName(mainBundleName),
					() -> loadBundleWithPath(userDir), () -> bundleForName("JavaFoundation")).orElse(null);
			if (mainBundle != null && !Objects.equals(mainBundle.name(), mainBundle.info().nsExecutable())) {
				throw new IllegalStateException("There was no name defined for the bundle '" + mainBundle + "'");
			}
		}
		return mainBundle;
	}

	private static URI uriForBundlePath(final String bundlePath) {
		final File file = new File(bundlePath);
		final URI uri;
		try {
			if (file.exists() && file.isFile() && file.canRead() && file.getName().endsWith(".jar")) {
				uri = new URI("jar", file.toURI().toString() + "!/", null);
			} else if (file.exists() && file.isDirectory() && file.canExecute()) {
				uri = file.toURI();
			} else {
				throw new IllegalArgumentException("Not a valid path for bundle file system " + bundlePath);
			}
			return uri;
		} catch (final URISyntaxException e) {
			throw NSForwardException._runtimeExceptionForThrowable(e);
		}
	}

	private static URL urlForBundlePath(final String bundlePath) {
		try {
			return uriForBundlePath(bundlePath).toURL();
		} catch (final MalformedURLException e) {
			throw NSForwardException._runtimeExceptionForThrowable(e);
		}
	}

	private final FileSystem bundleFileSystem;
	private final String bundlePath;
	private final NSBundleAdaptorProvider adaptor;
	private final Path fsBundlePath;
	private final Function<String, Predicate<Path>> isNamed;
	private final Function<String, Predicate<Path>> isExtension;
	private final Function<Path, String> pathToString;

	private NSArray<String> bundleClassNames;
	private URL bundlePathURL;
	private String bundleUrlPrefix;
	private NSBundleInfo info;
	private String name;
	private NSArray<String> packages;
	private Class<?> principalClass;
	private Properties properties;

	public NSBundle(final FileSystem bundleFileSystem, final String bundlePath, final NSBundleAdaptorProvider adaptor) {
		this.bundleFileSystem = bundleFileSystem;
		this.bundlePath = adaptor.adaptorBundlePath(bundleFileSystem, bundlePath);
		this.adaptor = adaptor;
		fsBundlePath = adaptor.fsBundlePath(bundleFileSystem, this.bundlePath);
		/*
		 * com.sun.nio.zipfs.ZipFileSystem appends a trailing / on directory names while
		 * sun.nio.fs.UnixFileSystem does not. This file name mapper allows us to
		 * normalize names depending on the file system type.
		 */
		final Function<Path, String> fileNameMapper = this.bundlePath.endsWith(".jar")
				? _NSPathUtils.FILE_NAME_TRIMMED.apply(this.bundleFileSystem.getSeparator())
				: _NSPathUtils.FILE_NAME;
		isNamed = _NSPathUtils.IS_NAMED.apply(fileNameMapper);
		isExtension = _NSPathUtils.IS_EXTENSION.apply(fileNameMapper);
		pathToString = this.bundlePath.endsWith(".jar")
				? _NSPathUtils.PATH_TO_STRING_TRIMMED.apply(this.bundleFileSystem.getSeparator())
				: _NSPathUtils.PATH_TO_STRING;
	}

	public String _bundleURLPrefix() {
		if (bundleUrlPrefix == null) {
			final URL url = urlForBundlePath(bundlePath());
			bundleUrlPrefix = url.toString();
		}
		return bundleUrlPrefix;
	}

	public Class<?> _classWithName(final String className) {
		Objects.requireNonNull(className, "Class name cannot be null.");
		return NSValueUtilities.coalesce(className,
				// Shallow search
				name -> _NSUtilities._classWithPartialName(name, false),
				// Search in packages
				name -> _NSUtilities._searchForClassInPackages(name, bundleClassPackageNames(), true, false),
				// Deep search
				name -> _NSUtilities._classWithPartialName(name, true))
				// or null
				.orElse(null);
	}

	public NSDictionary<String, Object> _infoDictionary() {
		return infoDictionary();
	}

	public void _simplePathsInDirectoryInJar(final String startPath, final String dirExtension,
			final NSMutableArray<String> dirs, final String fileExtension, final NSMutableArray<String> files) {
		final String start = startPath.length() == 0 || ".".equals(startPath) ? "/" : startPath;
		final Path dir = bundleFileSystem.getPath(start);
		if (Files.exists(dir)) {
			try {
				Files.list(dir).filter(Files::isDirectory).filter(path -> path.toString().endsWith(dirExtension))
						.map(path -> path.getFileName().toString()).forEach(dirs::add);
				Files.list(dir).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(fileExtension))
						.map(path -> path.getFileName().toString()).forEach(files::add);
			} catch (final IOException e) {
				throw NSForwardException._runtimeExceptionForThrowable(e);
			}
		}
	}

	public URL _urlForRelativePath(final String path) {
		return pathURLForResourcePath(path);
	}

	public NSArray<String> bundleClassNames() {
		if (bundleClassNames == null) {
			bundleClassNames = NSValueUtilities
					.mutableArrayFromStream(adaptor.classNamesForFileSystem(bundleFileSystem, fsBundlePath))
					.immutableClone();
			bundleClassNames.forEach(name -> BUNDLES_BY_CLASS_NAME.put(name, this));
		}
		return bundleClassNames;
	}

	public NSArray<String> bundleClassPackageNames() {
		if (packages == null) {
			packages = NSValueUtilities
					.mutableArrayFromStream(bundleClassNames().stream()
							.map(name -> _NSStringUtilities.stringByDeletingLastComponent(name, '.')).distinct())
					.immutableClone();
		}
		return packages;
	}

	public String bundlePath() {
		return bundlePath;
	}

	public URL bundlePathURL() {
		if (bundlePathURL == null) {
			try {
				bundlePathURL = new File(bundlePath()).toURI().toURL();
			} catch (final MalformedURLException e) {
				throw NSForwardException._runtimeExceptionForThrowable(e);
			}
		}
		return bundlePathURL;
	}

	public byte[] bytesForResourcePath(final String aResourcePath) {
		try (InputStream is = inputStreamForResourcePath(aResourcePath)) {
			return is == null ? new byte[0] : _NSStringUtilities.bytesFromInputStream(is);
		} catch (final IOException e) {
			throw NSForwardException._runtimeExceptionForThrowable(e);
		}
	}

	private NSBundleInfo info() {
		if (info == null) {
			info = adaptor.bundleInfoFromFileSystem(bundleFileSystem, fsBundlePath);
		}
		return info;
	}

	public NSDictionary<String, Object> infoDictionary() {
		return info().dictionary();
	}

	public InputStream inputStreamForResourcePath(final String aResourcePath) {
		final URL url = pathURLForResourcePath(aResourcePath);
		if (url != null) {
			try {
				return url.openStream();
			} catch (final IOException e) {
				throw NSForwardException._runtimeExceptionForThrowable(e);
			}
		}
		return null;
	}

	public boolean isFramework() {
		return !CFBundlePackageType.APPL.equals(info().cfBundlePackageType());
	}

	public boolean isJar() {
		return bundlePath().endsWith(".jar");
	}

	@Deprecated
	public boolean load() {
		return true;
	}

	public String name() {
		if (name == null) {
			String aName = info().nsExecutable();
			if (aName == null) {
				aName = new File(bundlePath()).getName();
				if (aName.contains(".") && aName.length() > 3) {
					aName = NSPathUtilities.stringByDeletingPathExtension(aName);
				}
			}
			name = aName;
		}
		return name;
	}

	@Deprecated
	public String pathForResource(final String aName, final String anExtension) {
		return pathForResource(aName, anExtension, null);
	}

	@Deprecated
	public String pathForResource(final String aName, final String anExtension, final String aSubDirPath) {
		// Nothing calls this, I'm not doing it
		throw new UnsupportedOperationException("Not implemented");
	}

	@Deprecated
	public NSArray<String> pathsForResources(final String anExtension, final String aSubDirPath) {
		// Nothing calls this, I'm not doing it
		throw new UnsupportedOperationException("Not implemented");
	}

	public URL pathURLForResourcePath(final String aResourcePath) {
		final String path = _NSPathUtils.originalResourcePath(aResourcePath);
		// For each resource path in the bundle
		String url = adaptor.resourcePathsForFileSystem(bundleFileSystem, fsBundlePath).stream()
				// Resolve the original path
				.map(p -> p.resolve(path))
				// Find the first existing
				.filter(Files::exists).findFirst()
				// Relativize it
				.map(fsBundlePath::relativize)
				// And return the string, or null of none exist
				.map(Path::toString).orElse(null);
		if (url == null && aResourcePath.startsWith(_NSPathUtils.NONLOCALIZED_LPROJ)) {
			/*
			 * Try again with the original path. There might actually be a
			 * Nonlocalized.lproj folder.
			 */
			url = adaptor.resourcePathsForFileSystem(bundleFileSystem, fsBundlePath).stream()
					// Resolve the original path
					.map(p -> p.resolve(aResourcePath))
					// Find the first existing
					.filter(Files::exists).findFirst()
					// Relativize it
					.map(fsBundlePath::relativize)
					// And return the string, or null of none exist
					.map(Path::toString).orElse(null);
		}
		try {
			return url == null ? null : new URL(_bundleURLPrefix().concat(url));
		} catch (final MalformedURLException e) {
			throw NSForwardException._runtimeExceptionForThrowable(e);
		}
	}

	private void postNotification() {
		NSNotificationCenter.defaultCenter().postNotification(BundleDidLoadNotification, this,
				new NSDictionary<>(bundleClassNames(), LoadedClassesNotification));
	}

	public Class<?> principalClass() {
		if (principalClass == null) {
			final String principalClassName = info().nsPrincipalClass();
			if (principalClassName != null && !"".equals(principalClassName)
					&& !"true".equals(System.getProperty("NSSkipPrincipalClasses"))) {
				principalClass = _NSUtilities.classWithName(principalClassName);
				if (principalClass == null && _NSUtilities._principalClassLoadingWarningsNeeded) {
					NSLog.err.appendln("Principal class '" + principalClassName + "' not found in bundle " + name());
					if (NSLog.debugLoggingAllowedForLevelAndGroups(NSLog.DebugLevelCritical,
							NSLog.DebugGroupResources)) {
						NSLog.debug.appendln(new ClassNotFoundException(principalClassName));
					}
				}
			}
		}
		return principalClass;
	}

	public Properties properties() {
		if (properties == null) {
			properties = adaptor.propertiesForFileSystem(bundleFileSystem, fsBundlePath);
		}
		return properties;
	}

	/**
	 * There may be more than one resource path (maven project), so you're just
	 * getting the first one in the list here... If it's a jar, you're getting a URL
	 * string. IDK why you are calling this method, it's deprecated for a reason.
	 *
	 * @return garbage
	 */
	@Deprecated
	public String resourcePath() {
		// there may be more than one, but just take the first one anyway...
		final Path resources = adaptor.resourcePathsForFileSystem(bundleFileSystem, fsBundlePath).get(0);
		final Path resolved = fsBundlePath.resolve(resources);
		return isJar() ? _bundleURLPrefix().concat(resolved.toString()) : resolved.toString();
	}

	public String resourcePathForLocalizedResourceNamed(final String aName, final String aSubDirPath) {
		final NSArray<String> result = resourcePathsForFilterAndLocale(isNamed.apply(aName), Locale.getDefault(),
				aSubDirPath);
		return result.isEmpty() ? null : result.get(0);
	}

	public NSArray<String> resourcePathsForDirectories(final String extension, final String aSubDirPath) {
		return resourcePathsForFilter(Files::isDirectory, extension, aSubDirPath);
	}

	private NSArray<String> resourcePathsForFilter(final Predicate<Path> aFilter, final String extension,
			final String aSubDirPath) {
		final List<Path> paths = adaptor.resourcePathsForFileSystem(bundleFileSystem, fsBundlePath);
		final String subDir = Optional.ofNullable(aSubDirPath).orElse("");
		final NSMutableArray<String> results = new NSMutableArray<>();

		// Filters
		final Predicate<Path> isExt = isExtension.apply(extension);

		// Mappers
		final BiFunction<Path, Path, Path> nonLocalize = _NSPathUtils::nonLocalizePath;

		final List<Path> walkPaths = paths.stream().map(fsBundlePath::resolve).collect(Collectors.toList());
		for (final Path walk : walkPaths) {
			final Path dir = walk.resolve(subDir);
			if (Files.exists(dir)) {
				try (Stream<Path> s = Files.walk(dir)) {
					s.filter(aFilter).filter(isExt).map(p -> nonLocalize.apply(walk, p)).map(pathToString)
							.forEach(results::add);
				} catch (final Exception e) {
					throw NSForwardException._runtimeExceptionForThrowable(e);
				}
			}
		}
		return results;
	}

	private NSArray<String> resourcePathsForFilterAndLocale(final Predicate<Path> aFilter, final Locale locale,
			final String aSubDirPath) {
		final List<Path> paths = adaptor.resourcePathsForFileSystem(bundleFileSystem, fsBundlePath);
		final String subDir = Optional.ofNullable(aSubDirPath).orElse("");
		final ConcurrentHashMap<Path, String> results = new ConcurrentHashMap<>();

		// Filters
		final Predicate<Path> isFile = Files::isRegularFile;

		// Path processing
		final BiConsumer<Path, Path> load = (basePath, path) -> _NSPathUtils.loadPathResultForLocale(results, locale,
				basePath, path);

		final List<Path> walkPaths = paths.stream().map(fsBundlePath::resolve).collect(Collectors.toList());
		for (final Path walk : walkPaths) {
			final Path dir = walk.resolve(subDir);
			if (Files.exists(dir)) {
				try (Stream<Path> s = Files.walk(dir)) {
					s.filter(aFilter).filter(isFile).forEach(p -> load.accept(walk, p));
				} catch (final Exception e) {
					throw NSForwardException._runtimeExceptionForThrowable(e);
				}
			}
		}

		return _NSPathUtils.unloadPathResults(results, pathToString);
	}

	public NSArray<String> resourcePathsForLocalizedResources(final String extension, final String aSubDirPath) {
		final Predicate<Path> filter = isExtension.apply(extension);
		return resourcePathsForFilterAndLocale(filter, Locale.getDefault(), aSubDirPath);
	}

	public NSArray<String> resourcePathsForResources(final String extension, final String aSubDirPath) {
		return resourcePathsForFilter(Files::isRegularFile, extension, aSubDirPath);
	}

	public NSArray<String> resourcePathsForResourcesNamed(final String name) {
		// New method for WODeployedBundle to use
		final Path resourcePath = bundleFileSystem.getPath(name);
		final String fileName = pathToString.apply(resourcePath.getFileName());
		final int len = resourcePath.getNameCount();
		final Path head = len > 1 ? resourcePath.subpath(0, len - 1) : null;
		final String subDir = Optional.ofNullable(head).map(pathToString).orElse(null);
		return resourcePathsForFilter(isNamed.apply(fileName), null, subDir);
	}

	/**
	 * The string returned includes the receiver's class name (NSBundle or a
	 * subclass), its name, its path, the names of its packages (as returned by
	 * bundleClassPackageNames), and the number of classes it contains.
	 *
	 * @return String giving details of this NSBundle object
	 * @see {@link #bundleClassPackageNames()}, {@link #name()}
	 */
	@Override
	public String toString() {
		return "<" + getClass().getName() + " name:'" + name() + "' bundlePath:'" + bundlePath() + "' packages:'"
				+ bundleClassPackageNames() + "' " + bundleClassNames().count() + " classes >";
	}

}
