package org.robolectric.internal;

import static java.util.Collections.emptyList;

import java.net.URL;
import java.nio.file.Path;
import java.util.Properties;
import org.robolectric.annotation.Config;
import org.robolectric.res.Fs;
import org.robolectric.util.Logger;

@SuppressWarnings("NewApi")
public class DefaultManifestFactory implements ManifestFactory {
  private final Properties properties;

  public DefaultManifestFactory(Properties properties) {
    this.properties = properties;
  }

  @Override
  public ManifestIdentifier identify(Config config) {
    Path manifestFile = getFileFromProperty("android_merged_manifest");
    Path resourcesDir = getFileFromProperty("android_merged_resources");
    Path assetsDir = getFileFromProperty("android_merged_assets");
    Path apkFile = getFileFromProperty("android_resource_apk");
    String packageName = properties.getProperty("android_custom_package");

    String manifestConfig = config.manifest();
    if (Config.NONE.equals(manifestConfig)) {
      Logger.info(
          "@Config(manifest = Config.NONE) specified while using Build System API, ignoring");
    } else if (!Config.DEFAULT_MANIFEST_NAME.equals(manifestConfig)) {
      manifestFile = getResource(manifestConfig);
    }

    return new ManifestIdentifier(
        packageName, manifestFile, resourcesDir, assetsDir, emptyList(), apkFile);
  }

  private Path getResource(String pathStr) {
    URL manifestUrl = getClass().getClassLoader().getResource(pathStr);
    if (manifestUrl == null) {
      throw new IllegalArgumentException("couldn't find '" + pathStr + "'");
    } else {
      return Fs.fromUrl(manifestUrl);
    }
  }

  private Path getFileFromProperty(String propertyName) {
    String path = properties.getProperty(propertyName);
    if (path == null || path.isEmpty()) {
      return null;
    }

    return Fs.fromUrl(path);
  }
}
