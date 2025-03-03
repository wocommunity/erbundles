package com.webobjects.foundation.development;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.stream.Stream;

/**
 * <p>
 * This interface exists to support multiple bundle layouts. NSBundle produces a
 * view of a bundle laid out by .lproj directories. However, the actual layout
 * of bundle resources is very different. This adaptor provides the abstraction
 * necessary to map the real paths into the NSBundle's imagined view of the
 * layout.
 * </p>
 *
 * <p>
 * By providing these general layout hints, the NSBundle can read resources from
 * a bundle whether it is a directory or just a jar file using the Java
 * {@link FileSystem} objects. The important but confusing paths are the
 * following.
 * </p>
 *
 * <p>
 * A <code>bundlePath</code>, which is the actual path initially passed into the
 * adaptor. It may be something like /path/to/bundle/ or /path/to/bundle.jar or
 * even a workspace project on the classpath like
 * /path/to/bundle/target/classes/.
 * </p>
 *
 * <p>
 * An <code>adaptorBundlePath</code>. Given the above bundlePath, the adaptor
 * needs to find the real bundle path. Examples would be /path/to/bundle/ or
 * /path/to/bundle.jar or /path/to/bundle/build/bundle.framework/ respectively
 * for the given bundle paths mentioned previously.
 * </p>
 *
 * <p>
 * Finally, there is a <code>fsBundlePath</code> which is the path to the bundle
 * in the {@link FileSystem} as a {@link Path} object. For a jar bundle, this is
 * just '/' but for other bundles, it is a path to the adaptorBundlePath.
 * </p>
 *
 * <p>
 * Bundle adaptors are loaded by NSBundle using a {@link ServiceLoader}. Which
 * means if you want to provide your own custom bundle layout, then you would
 * implement this interface, then place the fully qualified className for your
 * implementation in your jar's META-INF/services/ directory in a file named
 * {@link com.webobjects.foundation.development.NSBundleAdaptorProvider}. For
 * maven users, you can simply create a META-INF/services/ directory in your
 * project's src/main/resources directory and maven will copy it there for you
 * automatically. For Java 9+, you may need to do this differently if you use
 * Java jigsaw modules.
 * </p>
 *
 * <p>
 * Once loaded by the {@link ServiceLoader}, NSBundle will simply use the first
 * adaptor it finds where {@link #isAdaptable(FileSystem, String)} is true.
 * </p>
 */
public interface NSBundleAdaptorProvider {

	/**
	 * This method locates the adapted bundle path given an initial bundlePath
	 * argument. For example, a WOLips project may produce a build/ directory
	 * containing a build bundle. Given a path into the bundle project, this method
	 * can locate the adapted build/ bundle.
	 *
	 * @param fs         the FileSystem for the bundle
	 * @param bundlePath the original path
	 * @return the adaptor path
	 */
	String adaptorBundlePath(FileSystem fs, String bundlePath);

	/**
	 * Read or create the infoDictionary for a bundle.
	 *
	 * @param fs           the file system for the bundle
	 * @param fsBundlePath the root path of the bundle in the file system
	 * @return the bundle info
	 */
	NSBundleInfo bundleInfoFromFileSystem(FileSystem fs, Path fsBundlePath);

	/**
	 * Reads a stream of available class names from the bundle.
	 *
	 * @param fs
	 * @param fsBundlePath
	 * @return
	 */
	Stream<String> classNamesForFileSystem(FileSystem fs, Path fsBundlePath);

	/**
	 * Provides the file system relative bundle path given the adaptor path. For
	 * example, a jar bundle acts something like a chroot, so the fsBundlePath would
	 * just be '/'
	 *
	 * @param fs                the bundle file system
	 * @param adaptorBundlePath the path handled by the adaptor
	 * @return
	 */
	Path fsBundlePath(FileSystem fs, String adaptorBundlePath);

	/**
	 * @param fs
	 * @param bundlePath
	 * @return true if the adaptor handles the given bundlePath
	 */
	boolean isAdaptable(FileSystem fs, String bundlePath);

	/**
	 * Reads properties from the bundle.
	 *
	 * @param fs
	 * @param fsBundlePath
	 * @return
	 */
	Properties propertiesForFileSystem(FileSystem fs, Path fsBundlePath);

	/**
	 * Provides a list of relative resource paths where bundle resources may be
	 * found.
	 *
	 * @param fs
	 * @param fsBundlePath
	 * @return
	 */
	List<Path> resourcePathsForFileSystem(FileSystem fs, Path fsBundlePath);
}
