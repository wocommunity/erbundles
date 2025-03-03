package com.webobjects.appserver._private;

import java.util.Optional;

import com.webobjects._ideservices._IDEProject;
import com.webobjects._ideservices._WOProject;
import com.webobjects.foundation.NSBundle;
import com.webobjects.foundation.NSLog;
import com.webobjects.foundation.NSPathUtilities;
import com.webobjects.foundation.NSProperties;
import com.webobjects.foundation.NSPropertyListSerialization;

public class WOProjectBundle extends WODeployedBundle {

	private static volatile boolean _refreshProjectBundlesOnCacheMiss = NSPropertyListSerialization
			.booleanForString(NSProperties.getProperty("WOMissingResourceSearchEnabled"));

	protected static boolean _isProjectBundlePath(final String aProjectDirectoryPath) {
		final _IDEProject ideProject = _WOProject.ideProjectAtPath(aProjectDirectoryPath);
		return (ideProject != null);
	}

	public static synchronized WODeployedBundle bundleWithPath(final String aPath) {
		return WODeployedBundle.bundleWithPath(aPath);
	}

	public static WOProjectBundle projectBundleForProject(final String aProjectName, final boolean shouldBeFramework) {
		return Optional.ofNullable(aProjectName).map(NSBundle::bundleForName).map(WODeployedBundle::bundleWithNSBundle)
				.map(WODeployedBundle::projectBundle).orElse(null);
	}

	public static boolean refreshProjectBundlesOnCacheMiss() {
		return _refreshProjectBundlesOnCacheMiss;
	}

	public static void setRefreshProjectBundlesOnCacheMiss(final boolean refresh) {
		_refreshProjectBundlesOnCacheMiss = refresh;
	}

	private volatile _WOProject _woProject;

	private final String _projectPath;

	private final WODeployedBundle _associatedDeployedBundle;

	public WOProjectBundle(final String aProjectPath, final WODeployedBundle aDeployedBundle) {
		super(aProjectPath, aDeployedBundle.nsBundle());
		String projectPath = _woProject.ideProject().ideProjectPath();
		if (projectPath.endsWith("PB.project")) {
			projectPath = NSPathUtilities.stringByDeletingLastPathComponent(projectPath);
		} else if (projectPath.endsWith("project.pbxproj")) {
			projectPath = NSPathUtilities.stringByDeletingLastPathComponent(projectPath);
			if (projectPath.endsWith(".pbproj")) {
				projectPath = NSPathUtilities.stringByDeletingLastPathComponent(projectPath);
			}
			if (projectPath.endsWith(".xcodeproj")) {
				projectPath = NSPathUtilities.stringByDeletingLastPathComponent(projectPath);
			}
			if (projectPath.endsWith(".xcode")) {
				projectPath = NSPathUtilities.stringByDeletingLastPathComponent(projectPath);
			}
		}
		_projectPath = projectPath;
		if (_projectName == null && NSLog._debugLoggingAllowedForLevelAndGroups(NSLog.DebugLevelDetailed,
				NSLog.DebugGroupResources | NSLog.DebugGroupWebObjects)) {
			NSLog.debug.appendln("<" + getClass().getName() + ">: Warning - Unable to locate PROJECTNAME in '"
					+ aDeployedBundle.bundlePath() + "'");
		}
		_associatedDeployedBundle = aDeployedBundle;
	}

	@Override
	protected String _initBundlePath(final String aPath) {
		String path = null;
		final _WOProject project = _WOProject.projectAtPath(aPath);
		if (project != null) {
			path = project.bundlePath();
			_woProject = project;
		}
		if (path == null) {
			path = NSPathUtilities.stringByNormalizingExistingPath(aPath);
		}
		return path;
	}

	@Override
	protected String _initProjectName(final String aProjectName) {
		if (_woProject != null) {
			return _woProject.ideProject().projectName();
		}
		return null;
	}

	public _WOProject _woProject() {
		return _woProject;
	}

	@Override
	public WOProjectBundle projectBundle() {
		return this;
	}

	public String projectPath() {
		return _projectPath;
	}

	@Override
	public String toString() {
		return "<" + getClass().getName() + ": projectName='" + _projectName + "'; bundlePath='" + _bundlePath
				+ "'; projectPath='" + _projectPath + "'>";
	}
}