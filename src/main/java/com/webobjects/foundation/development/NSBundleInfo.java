package com.webobjects.foundation.development;

import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;

public class NSBundleInfo {
	public enum CFBundlePackageType {
		/**
		 * The application package type value.
		 */
		APPL,
		/**
		 * The framework package type value.
		 */
		FMWK;
	}

	public static final String CF_BUNDLE_NAME_KEY = "CFBundleName";
	public static final String CF_BUNDLE_IDENTIFIER_KEY = "CFBundleIdentifier";
	public static final String CF_BUNDLE_SHORT_VERSION_STRING_KEY = "CFBundleShortVersionString";
	public static final String CF_BUNDLE_INFO_DICTIONARY_VERSION_KEY = "CFBundleInfoDictionaryVersion";
	public static final String NS_JAVA_PATH_CLIENT_KEY = "NSJavaPathClient";
	public static final String NS_JAVA_CLIENT_ROOT_KEY = "NSJavaClientRoot";
	public static final String CF_BUNDLE_ICON_FILE_KEY = "CFBundleIconFile";
	public static final String NS_JAVA_NEEDED_KEY = "NSJavaNeeded";
	public static final String CF_BUNDLE_DEVELOPMENT_REGION_KEY = "CFBundleDevelopmentRegion";
	public static final String CF_BUNDLE_EXECUTABLE_KEY = "CFBundleExecutable";
	public static final String NS_PRINCIPAL_CLASS_KEY = "NSPrincipalClass";
	public static final String NOTE_KEY = "NOTE";
	public static final String NS_JAVA_ROOT_KEY = "NSJavaRoot";
	public static final String HAS_WOCOMPONENTS_KEY = "Has_WOComponents";
	public static final String NS_JAVA_PATH_KEY = "NSJavaPath";
	public static final String CF_BUNDLE_PACKAGE_TYPE_KEY = "CFBundlePackageType";
	public static final String CF_BUNDLE_SIGNATURE_KEY = "CFBundleSignature";
	public static final String NS_EXECUTABLE_KEY = "NSExecutable";
	public static final String MANIFEST_IMPLEMENTATION_VERSION_KEY = "Implementation-Version";
	public static final String EO_ADAPTOR_CLASS_NAME_KEY = "EOAdaptorClassName";

	public static NSBundleInfo forDictionary(final NSDictionary<String, Object> plist) {
		final NSBundleInfo info = new NSBundleInfo();
		info.dictionary = plist.mutableClone();
		return info;
	}

	private NSMutableDictionary<String, Object> dictionary;

	private NSBundleInfo() {
	}

	public String cfBundleDevelopmentRegion() {
		return (String) dictionary.getOrDefault(CF_BUNDLE_DEVELOPMENT_REGION_KEY, "English");
	}

	public String cfBundleExecutable() {
		return (String) dictionary.objectForKey(CF_BUNDLE_EXECUTABLE_KEY);
	}

	public String cfBundleIconFile() {
		return (String) dictionary.objectForKey(CF_BUNDLE_ICON_FILE_KEY);
	}

	public String cfBundleIdentifier() {
		return (String) dictionary.objectForKey(CF_BUNDLE_IDENTIFIER_KEY);
	}

	public String cfBundleInfoDictionaryVersion() {
		return (String) dictionary.objectForKey(CF_BUNDLE_INFO_DICTIONARY_VERSION_KEY);

	}

	public String cfBundleName() {
		return (String) dictionary.objectForKey(CF_BUNDLE_NAME_KEY);
	}

	public CFBundlePackageType cfBundlePackageType() {
		for (final CFBundlePackageType type : CFBundlePackageType.values()) {
			if (type.name().equals(dictionary.objectForKey(CF_BUNDLE_PACKAGE_TYPE_KEY))) {
				return type;
			}
		}
		return null;
	}

	public String cfBundleShortVersionString() {
		return (String) dictionary.objectForKey(CF_BUNDLE_SHORT_VERSION_STRING_KEY);
	}

	public String cfBundleSignature() {
		return (String) dictionary.objectForKey(CF_BUNDLE_SIGNATURE_KEY);
	}

	public NSDictionary<String, Object> dictionary() {
		return dictionary.immutableClone();
	}

	public Boolean hasWOComponents() {
		return (Boolean) dictionary.objectForKey(HAS_WOCOMPONENTS_KEY);
	}

	public String manifestImplementationVersion() {
		return (String) dictionary.objectForKey(MANIFEST_IMPLEMENTATION_VERSION_KEY);
	}

	public String note() {
		return (String) dictionary.objectForKey(NOTE_KEY);
	}

	public String nsExecutable() {
		return (String) dictionary.objectForKey(NS_EXECUTABLE_KEY);
	}

	public String nsJavaClientRoot() {
		return (String) dictionary.objectForKey(NS_JAVA_CLIENT_ROOT_KEY);
	}

	public Boolean nsJavaNeeded() {
		return (Boolean) dictionary.objectForKey(NS_JAVA_NEEDED_KEY);
	}

	public NSArray<String> nsJavaPath() {
		return (NSArray) dictionary.objectForKey(NS_JAVA_PATH_KEY);
	}

	public String nsJavaPathClient() {
		return (String) dictionary.objectForKey(NS_JAVA_PATH_CLIENT_KEY);
	}

	public String nsJavaRoot() {
		return (String) dictionary.objectForKey(NS_JAVA_ROOT_KEY);
	}

	public String nsPrincipalClass() {
		return (String) dictionary.objectForKey(NS_PRINCIPAL_CLASS_KEY);
	}
}
