package com.webobjects.foundation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Stack;

public class NSNestedProperties extends Properties {
	private static final long serialVersionUID = -4001826101238711359L;

	public static final String IncludePropsKey = ".includeProps";

	public static final String IncludePropsSoFarKey = ".includePropsSoFar";

	private final Stack<File> _files = new Stack<>();

	public NSNestedProperties() {
	}

	public NSNestedProperties(final Properties defaults) {
		super(defaults);
	}

	public synchronized void load(final File propsFile) throws IOException {
		File canonicalPropsFile;
		NSLog.out.appendln("NSNestedProperties.load(): " + propsFile);
		canonicalPropsFile = propsFile.getCanonicalFile();
		_files.push(canonicalPropsFile.getParentFile());
		try {
			try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(canonicalPropsFile))) {
				load(is);
			}
		} finally {
			_files.pop();
		}
	}

	@Override
	public synchronized Object put(final Object key, final Object value) {
		if (IncludePropsKey.equals(key)) {
			String propsPath;
			final String propsFileName = (String) value;
			File propsFile = new File(propsFileName);
			if (!propsFile.isAbsolute()) {
				File cwd = null;
				if (_files.size() > 0) {
					cwd = _files.peek();
				} else {
					cwd = new File(System.getProperty("user.home"));
				}
				propsFile = new File(cwd, propsFileName);
			}
			try {
				propsPath = propsFile.getCanonicalPath();
			} catch (final IOException e) {
				throw new RuntimeException("Failed to canonicalize the property file '" + propsFile + "'.", e);
			}
			String existingIncludeProps = getProperty(IncludePropsSoFarKey);
			if (existingIncludeProps == null) {
				existingIncludeProps = "";
			}
			if (existingIncludeProps.indexOf(propsPath) > -1) {
				NSLog.err.appendln("NSNestedProperties.load(): Possible recursive includeProps detected. '" + propsPath
						+ "' was included in more than one of the following files: " + existingIncludeProps);
				NSLog.err.appendln("NSNestedProperties.load() cannot proceed - QUITTING!");
				System.exit(1);
			}
			if (existingIncludeProps.length() > 0) {
				existingIncludeProps = String.valueOf(existingIncludeProps) + ", ";
			}
			existingIncludeProps = String.valueOf(existingIncludeProps) + propsPath;
			super.put(IncludePropsSoFarKey, existingIncludeProps);
			try {
				load(propsFile);
			} catch (final IOException e) {
				throw new RuntimeException("Failed to load the property file '" + value + "'.", e);
			}
			return null;
		}
		return super.put(key, value);
	}

}
