package com.webobjects.appserver._private;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSBundle;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSPathUtilities;
import com.webobjects.foundation.NSProperties;
import com.webobjects.foundation.NSPropertyListSerialization;
import com.webobjects.foundation._NSPathUtils;

public class WODeployedBundle {
	/**
	 * This was originally used in {@link #bundleWithNSBundle(NSBundle)} to load
	 * {@link WOProjectBundle}s.
	 */
	private static final boolean _allowRapidTurnaround = NSPropertyListSerialization
			.booleanForString(NSProperties.getProperty("WOAllowRapidTurnaround"));

	private static final ConcurrentHashMap<NSBundle, WODeployedBundle> DEPLOYED_BUNDLES = new ConcurrentHashMap<>(
			NSBundle.frameworkBundles().count());

	public static WODeployedBundle bundleWithNSBundle(final NSBundle bundle) {
		/*
		 * The WOProjectBundle seems really quite unnecessary. Let's pretend it doesn't
		 * exist and see how it goes.
		 */
		return DEPLOYED_BUNDLES.computeIfAbsent(bundle, WODeployedBundle::new);
	}

//	public static WODeployedBundle bundleWithNSBundle(final NSBundle bundle) {
//		return DEPLOYED_BUNDLES.computeIfAbsent(bundle, nsBundle -> {
//			WODeployedBundle deployedBundle = new WODeployedBundle(nsBundle);
//			if (_allowRapidTurnaround) {
//				final String bundlePath = nsBundle.bundlePath();
//				try {
//					if (WOProjectBundle._isProjectBundlePath(bundlePath)) {
//						deployedBundle.projectBundle = new WOProjectBundle(bundlePath, deployedBundle);
//						deployedBundle = deployedBundle.projectBundle;
//					}
//				} catch (final Exception e) {
//					if (NSLog.debugLoggingAllowedForLevel(NSLog.DebugLevelCritical)) {
//						NSLog.debug.appendln("<WOProjectBundle>: Warning - Unable to find project at path "
//								+ nsBundle.bundlePathURL().getPath() + " - Ignoring project.");
//						NSLog.debug.appendln(e);
//					}
//				}
//			}
//			return deployedBundle;
//		});
//	}

	public static WODeployedBundle bundleWithPath(final String aPath) {
		return Optional.ofNullable(aPath).map(NSBundle::bundleWithPath).map(WODeployedBundle::bundleWithNSBundle)
				.orElse(null);
	}

	public static WODeployedBundle deployedBundle() {
		return bundleWithNSBundle(NSBundle.mainBundle());
	}

	public static WODeployedBundle deployedBundleForFrameworkNamed(final String aFrameworkName) {
		return Optional.ofNullable(aFrameworkName).map(NSBundle::bundleForName)
				.map(WODeployedBundle::bundleWithNSBundle).orElse(null);
	}

	protected final String _bundlePath;

	protected final boolean _isFramework;

	private final NSBundle nsBundle;

	protected final String _projectName;

	protected final String _wrapperName;

	/**
	 * If we actually want to load one of these, it could be done in
	 * {@link #bundleWithNSBundle(NSBundle)}
	 */
	private WOProjectBundle projectBundle;

	public WODeployedBundle(final NSBundle nsb) {
		this(null, nsb);
	}

	public WODeployedBundle(final String projectPath, final NSBundle bundle) {
		Objects.requireNonNull(bundle, "Cannot create WODeployedBundle without corresponding NSBundle!");
		nsBundle = bundle;
		final String aProjectPath = (projectPath != null) ? projectPath : nsBundle.bundlePath();
		_bundlePath = _initBundlePath(aProjectPath);
		if (NSBundle.mainBundle() == nsBundle || (!nsBundle.isFramework() && !nsBundle.name().endsWith(".woa"))) {
			_wrapperName = nsBundle.name() + ".woa";
		} else if (nsBundle.isFramework() && !nsBundle.name().endsWith(".framework")) {
			_wrapperName = nsBundle.name() + ".framework";
		} else {
			_wrapperName = nsBundle.name();
		}
		_projectName = _initProjectName(_wrapperName);
		if (NSBundle.mainBundle() == nsBundle) {
			_isFramework = false;
		} else {
			_isFramework = nsBundle.isFramework();
		}
	}

	private String _absolutePathForRelativePath(final String aRelativePath) {
		if (isJar()) {
			throw new UnsupportedOperationException("Path operations not supported on jar bundles! Path was: "
					+ aRelativePath + " for bundle named " + nsBundle.name());
		}
		return Optional.ofNullable(aRelativePath).map(nsBundle::pathURLForResourcePath).map(URL::getPath).orElse(null);
	}

	public String _absolutePathForResource(final String aResourceName, final NSArray<String> aLanguagesList) {
		final String aRelativePath = relativePathForResource(aResourceName, aLanguagesList);
		return _absolutePathForRelativePath(aRelativePath);
	}

	public String _absolutePathForResource(final String aResourceName, final String aLanguage) {
		final String aRelativePath = relativePathForResource(aResourceName, aLanguage);
		return _absolutePathForRelativePath(aRelativePath);
	}

	public String _absolutePathForResource(final String aResourceName, final String aLanguage, final boolean refresh) {
		return _absolutePathForResource(aResourceName, aLanguage);
	}

	public NSArray<String> _allResourceNamesWithExtension(final String extension,
			final boolean webServerResourcesOnly) {
		throw new UnsupportedOperationException("Unimplemented");
	}

	protected String _initBundlePath(final String aPath) {
		if (!isJar()) {
			return NSPathUtilities.stringByNormalizingExistingPath(aPath);
		}
		return aPath;
	}

	protected URL _initBundleURL(final URL anURL) {
		return anURL;
	}

	protected String _initProjectName(final String aProjectName) {
		return NSPathUtilities.stringByDeletingPathExtension(aProjectName);
	}

	protected void _jarPreloadAllResourcesInSubDirectory(final String aDirectory, final NSMutableArray<String> array) {
	}

	protected void _newPreloadAllResourcesInSubDirectory(final File aDir, final int pathIndex, final int keyPathIndex,
			final NSMutableArray<String> array) {
	}

	public String bundlePath() {
		return _bundlePath;
	}

	public InputStream inputStreamForResourceNamed(final String aResourceName, final NSArray<String> aLanguagesList) {
		InputStream is = null;
		final URL url = pathURLForResourceNamed(aResourceName, aLanguagesList);
		if (url != null) {
			try {
				is = url.openStream();
			} catch (final IOException ioe) {
			}
		}
		return is;
	}

	public InputStream inputStreamForResourceNamed(final String aResourceName, final String aLanguage) {
		InputStream is = null;
		final URL url = pathURLForResourceNamed(aResourceName, aLanguage);
		if (url != null) {
			try {
				is = url.openStream();
			} catch (final IOException ioe) {
			}
		}
		return is;
	}

	public boolean isAggregate() {
		return false;
	}

	public boolean isFramework() {
		return _isFramework;
	}

	public boolean isJar() {
		return nsBundle.isJar();
	}

	public NSBundle nsBundle() {
		return nsBundle;
	}

	public URL pathURLForResourceNamed(final String aResourceName, final NSArray<String> aLanguagesList) {
		return Optional.ofNullable(relativePathForResource(aResourceName, aLanguagesList))
				.map(nsBundle::pathURLForResourcePath).orElse(null);
	}

	public URL pathURLForResourceNamed(final String aResourceName, final String aLanguageString) {
		return Optional.ofNullable(relativePathForResource(aResourceName, aLanguageString))
				.map(nsBundle::pathURLForResourcePath).orElse(null);
	}

	public URL pathURLForResourceNamed(final String aResourceName, final String aLanguageString,
			final boolean refreshProjectOnCacheMiss) {
		return pathURLForResourceNamed(aResourceName, aLanguageString);
	}

	public WOProjectBundle projectBundle() {
		return projectBundle;
	}

	public String projectName() {
		return _projectName;
	}

	public String relativePathForResource(final String aResourceName, final NSArray<String> aLanguagesList) {
		final NSArray<String> resourcePaths = nsBundle.resourcePathsForResourcesNamed(aResourceName);
		if (aLanguagesList != null) {
			for (final String lang : aLanguagesList) {
				final Optional<String> path = resourcePaths.stream()
						.filter(p -> p.startsWith(lang + _NSPathUtils.LPROJSUFFIX)).findFirst();
				if (path.isPresent()) {
					return path.get();
				}
			}
		}
		return resourcePaths.stream().filter(p -> p.startsWith(_NSPathUtils.NONLOCALIZED_LPROJ)).findFirst()
				.orElse(null);
	}

	public String relativePathForResource(final String aResourceName, final String aLanguageString) {
		final NSArray<String> resourcePaths = nsBundle.resourcePathsForResourcesNamed(aResourceName);
		return Optional.ofNullable(aLanguageString).flatMap(
				lang -> resourcePaths.stream().filter(p -> p.startsWith(lang + _NSPathUtils.LPROJSUFFIX)).findFirst())
				.orElseGet(() -> resourcePaths.stream().filter(p -> p.startsWith(_NSPathUtils.NONLOCALIZED_LPROJ))
						.findFirst().orElse(null));
	}

	@Override
	public String toString() {
		return "<" + getClass().getName() + ": bundlePath='" + _bundlePath + "'>";
	}

	public String urlForResource(final String aResourceName, final NSArray<String> aLanguagesList) {
		return Optional.ofNullable(relativePathForResource(aResourceName, aLanguagesList))
				.map(nsBundle::pathURLForResourcePath).map(URL::toString).orElse(null);
	}

	public String wrapperName() {
		return _wrapperName;
	}

}