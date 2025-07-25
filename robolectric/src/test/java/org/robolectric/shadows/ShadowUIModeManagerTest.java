package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.R;
import static android.os.Build.VERSION_CODES.S;
import static android.os.Build.VERSION_CODES.TIRAMISU;
import static android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.robolectric.Shadows.shadowOf;

import android.app.UiModeManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.provider.Settings;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Tests for {@link ShadowUIModeManager}. */
@RunWith(AndroidJUnit4.class)
public class ShadowUIModeManagerTest {
  private Context context;
  private UiModeManager uiModeManager;
  private ShadowUIModeManager shadowUiModeManager;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
    uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
    shadowUiModeManager = shadowOf(uiModeManager);
  }

  @Test
  @Config(minSdk = M)
  public void testModeSwitch() {
    assertThat(uiModeManager.getCurrentModeType()).isEqualTo(Configuration.UI_MODE_TYPE_UNDEFINED);
    assertThat(shadowUiModeManager.getLastFlags()).isEqualTo(0);

    uiModeManager.enableCarMode(1);
    assertThat(uiModeManager.getCurrentModeType()).isEqualTo(Configuration.UI_MODE_TYPE_CAR);
    assertThat(shadowUiModeManager.getLastFlags()).isEqualTo(1);

    uiModeManager.disableCarMode(2);
    assertThat(uiModeManager.getCurrentModeType()).isEqualTo(Configuration.UI_MODE_TYPE_NORMAL);
    assertThat(shadowUiModeManager.getLastFlags()).isEqualTo(2);
  }

  @Test
  public void testModeType() {
    assertThat(uiModeManager.getCurrentModeType()).isEqualTo(Configuration.UI_MODE_TYPE_UNDEFINED);
    shadowUiModeManager.setCurrentModeType(Configuration.UI_MODE_TYPE_DESK);
    assertThat(uiModeManager.getCurrentModeType()).isEqualTo(Configuration.UI_MODE_TYPE_DESK);
  }

  @Test
  @Config(minSdk = R)
  public void testCarModePriority() {
    int priority = 9;
    int flags = 1;
    uiModeManager.enableCarMode(priority, flags);
    assertThat(uiModeManager.getCurrentModeType()).isEqualTo(Configuration.UI_MODE_TYPE_CAR);
    assertThat(shadowUiModeManager.getLastCarModePriority()).isEqualTo(priority);
    assertThat(shadowUiModeManager.getLastFlags()).isEqualTo(flags);
  }

  private static final int INVALID_NIGHT_MODE = -4242;

  @Test
  public void testNightMode() {
    assertThat(uiModeManager.getNightMode()).isEqualTo(UiModeManager.MODE_NIGHT_AUTO);

    uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_YES);
    assertThat(uiModeManager.getNightMode()).isEqualTo(UiModeManager.MODE_NIGHT_YES);

    uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_NO);
    assertThat(uiModeManager.getNightMode()).isEqualTo(UiModeManager.MODE_NIGHT_NO);

    uiModeManager.setNightMode(INVALID_NIGHT_MODE);
    assertThat(uiModeManager.getNightMode()).isEqualTo(UiModeManager.MODE_NIGHT_AUTO);

    if (RuntimeEnvironment.getApiLevel() >= R) {
      uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_CUSTOM);
      assertThat(uiModeManager.getNightMode()).isEqualTo(UiModeManager.MODE_NIGHT_CUSTOM);
    }
  }

  @Test
  @Config(minSdk = S)
  public void testGetApplicationNightMode() {
    uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_YES);

    assertThat(shadowUiModeManager.getApplicationNightMode())
        .isEqualTo(UiModeManager.MODE_NIGHT_YES);
  }

  @Test
  @Config(minSdk = S)
  public void testRequestReleaseProjection() {
    shadowUiModeManager.setFailOnProjectionToggle(true);

    assertThrows(
        SecurityException.class,
        () -> uiModeManager.requestProjection(UiModeManager.PROJECTION_TYPE_AUTOMOTIVE));
    assertThat(shadowUiModeManager.getActiveProjectionTypes()).isEmpty();

    assertThrows(
        SecurityException.class,
        () -> uiModeManager.releaseProjection(UiModeManager.PROJECTION_TYPE_AUTOMOTIVE));
    assertThat(shadowUiModeManager.getActiveProjectionTypes()).isEmpty();

    setPermissions(android.Manifest.permission.TOGGLE_AUTOMOTIVE_PROJECTION);

    assertThat(uiModeManager.requestProjection(UiModeManager.PROJECTION_TYPE_AUTOMOTIVE)).isFalse();
    assertThat(shadowUiModeManager.getActiveProjectionTypes()).isEmpty();

    shadowUiModeManager.setFailOnProjectionToggle(false);

    assertThat(uiModeManager.requestProjection(UiModeManager.PROJECTION_TYPE_AUTOMOTIVE)).isTrue();
    assertThat(shadowUiModeManager.getActiveProjectionTypes())
        .containsExactly(UiModeManager.PROJECTION_TYPE_AUTOMOTIVE);
    assertThat(uiModeManager.requestProjection(UiModeManager.PROJECTION_TYPE_AUTOMOTIVE)).isTrue();
    assertThat(shadowUiModeManager.getActiveProjectionTypes())
        .containsExactly(UiModeManager.PROJECTION_TYPE_AUTOMOTIVE);

    shadowUiModeManager.setFailOnProjectionToggle(true);
    assertThat(uiModeManager.releaseProjection(UiModeManager.PROJECTION_TYPE_AUTOMOTIVE)).isFalse();
    assertThat(shadowUiModeManager.getActiveProjectionTypes())
        .containsExactly(UiModeManager.PROJECTION_TYPE_AUTOMOTIVE);

    shadowUiModeManager.setFailOnProjectionToggle(false);
    assertThat(uiModeManager.releaseProjection(UiModeManager.PROJECTION_TYPE_AUTOMOTIVE)).isTrue();
    assertThat(shadowUiModeManager.getActiveProjectionTypes()).isEmpty();
    assertThat(uiModeManager.releaseProjection(UiModeManager.PROJECTION_TYPE_AUTOMOTIVE)).isFalse();
    assertThat(shadowUiModeManager.getActiveProjectionTypes()).isEmpty();
  }

  @Config(minSdk = TIRAMISU)
  @Test
  public void getDefaultUiNightModeCustomType_shouldBeUnknownCustomType() {
    assertThat(uiModeManager.getNightModeCustomType())
        .isEqualTo(UiModeManager.MODE_NIGHT_CUSTOM_TYPE_UNKNOWN);
  }

  @Test
  public void getDefaultIsNightModeOn_shouldBeFalse() {
    assertThat(shadowUiModeManager.isNightModeOn()).isFalse();
  }

  @Config(minSdk = TIRAMISU)
  @Test
  public void setUiNightModeCustomType_validType_shouldGetSameCustomType() {
    uiModeManager.setNightModeCustomType(UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME);

    assertThat(uiModeManager.getNightMode()).isEqualTo(UiModeManager.MODE_NIGHT_CUSTOM);
    assertThat(uiModeManager.getNightModeCustomType())
        .isEqualTo(UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME);
  }

  @Config(minSdk = TIRAMISU)
  @Test
  public void setUiNightModeCustomType_invalidType_shouldGetUnknownCustomType() {
    uiModeManager.setNightModeCustomType(123);

    assertThat(uiModeManager.getNightModeCustomType())
        .isEqualTo(UiModeManager.MODE_NIGHT_CUSTOM_TYPE_UNKNOWN);
  }

  @Config(minSdk = TIRAMISU)
  @Test
  public void
      setNightModeActivatedForCustomMode_requestActivated_matchedCustomMode_shouldActivate() {
    uiModeManager.setNightModeCustomType(UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME);

    assertThat(
            uiModeManager.setNightModeActivatedForCustomMode(
                UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME, true))
        .isTrue();
    assertThat(shadowUiModeManager.isNightModeOn()).isTrue();
  }

  @Config(minSdk = TIRAMISU)
  @Test
  public void
      setNightModeActivatedForCustomMode_requestDeactivated_matchedCustomMode_shouldDeactivate() {
    uiModeManager.setNightModeCustomType(UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME);

    assertThat(
            uiModeManager.setNightModeActivatedForCustomMode(
                UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME, false))
        .isTrue();
    assertThat(shadowUiModeManager.isNightModeOn()).isFalse();
  }

  @Config(minSdk = TIRAMISU)
  @Test
  public void setNightModeActivatedForCustomMode_invalidTypeActivated_shouldNotActivate() {
    uiModeManager.setNightModeCustomType(UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME);

    assertThat(uiModeManager.setNightModeActivatedForCustomMode(123, true)).isFalse();
    assertThat(shadowUiModeManager.isNightModeOn()).isFalse();
  }

  @Config(minSdk = TIRAMISU)
  @Test
  public void setNightModeActivatedForCustomMode_differentCustomTypeActivated_shouldNotActivate() {
    uiModeManager.setNightModeCustomType(UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME);

    assertThat(
            uiModeManager.setNightModeActivatedForCustomMode(
                UiModeManager.MODE_NIGHT_CUSTOM_TYPE_SCHEDULE, true))
        .isFalse();
    assertThat(shadowUiModeManager.isNightModeOn()).isFalse();
  }

  @Config(minSdk = TIRAMISU)
  @Test
  public void setNightModeCustomTypeBedtime_setNightModeOff_shouldGetUnknownCustomType() {
    uiModeManager.setNightModeCustomType(UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME);

    uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_NO);

    assertThat(uiModeManager.getNightModeCustomType())
        .isEqualTo(UiModeManager.MODE_NIGHT_CUSTOM_TYPE_UNKNOWN);
  }

  @Config(minSdk = R)
  @Test
  public void setNightMode_setToYes_shouldUpdateSecureSettings() throws Exception {
    uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_YES);

    assertThat(Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.UI_NIGHT_MODE))
        .isEqualTo(UiModeManager.MODE_NIGHT_YES);
    assertThat(
            Settings.Secure.getInt(
                context.getContentResolver(), Settings.Secure.UI_NIGHT_MODE_CUSTOM_TYPE))
        .isEqualTo(UiModeManager.MODE_NIGHT_CUSTOM_TYPE_UNKNOWN);
  }

  @Config(minSdk = R)
  @Test
  public void setNightMode_setToNo_shouldUpdateSecureSettings() throws Exception {
    uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_NO);

    assertThat(Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.UI_NIGHT_MODE))
        .isEqualTo(UiModeManager.MODE_NIGHT_NO);
    assertThat(
            Settings.Secure.getInt(
                context.getContentResolver(), Settings.Secure.UI_NIGHT_MODE_CUSTOM_TYPE))
        .isEqualTo(UiModeManager.MODE_NIGHT_CUSTOM_TYPE_UNKNOWN);
  }

  @Config(minSdk = TIRAMISU)
  @Test
  public void setNightModeActivatedForCustomMode_setToBedtimeMode_shouldUpdateSecureSettings()
      throws Exception {
    uiModeManager.setNightModeCustomType(UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME);

    assertThat(
            Settings.Secure.getInt(
                context.getContentResolver(), Settings.Secure.UI_NIGHT_MODE_CUSTOM_TYPE))
        .isEqualTo(UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME);
  }

  @Config(minSdk = TIRAMISU)
  @Test
  public void setNightModeActivatedForCustomMode_setToSchedule_shouldUpdateSecureSettings()
      throws Exception {
    uiModeManager.setNightModeCustomType(UiModeManager.MODE_NIGHT_CUSTOM_TYPE_SCHEDULE);

    assertThat(
            Settings.Secure.getInt(
                context.getContentResolver(), Settings.Secure.UI_NIGHT_MODE_CUSTOM_TYPE))
        .isEqualTo(UiModeManager.MODE_NIGHT_CUSTOM_TYPE_SCHEDULE);
  }

  @Test
  @Config(minSdk = S)
  public void getProjectingPackages_noProjectingPackages_returnsEmpty() {
    assertThat(uiModeManager.getProjectingPackages(UiModeManager.PROJECTION_TYPE_ALL)).isEmpty();
  }

  @Test
  @Config(minSdk = S)
  public void getProjectingPackages_projecting_returnsNotEmpty() {
    setPermissions(android.Manifest.permission.TOGGLE_AUTOMOTIVE_PROJECTION);
    uiModeManager.requestProjection(UiModeManager.PROJECTION_TYPE_AUTOMOTIVE);

    assertThat(uiModeManager.getProjectingPackages(UiModeManager.PROJECTION_TYPE_AUTOMOTIVE))
        .contains(RuntimeEnvironment.getApplication().getPackageName());
  }

  @Test
  @Config(minSdk = S)
  public void getProjectingPackages_projecting_allTypes_returnsNotEmpty() {
    setPermissions(android.Manifest.permission.TOGGLE_AUTOMOTIVE_PROJECTION);
    uiModeManager.requestProjection(UiModeManager.PROJECTION_TYPE_AUTOMOTIVE);

    assertThat(uiModeManager.getProjectingPackages(UiModeManager.PROJECTION_TYPE_ALL))
        .contains(RuntimeEnvironment.getApplication().getPackageName());
  }

  @Test
  @Config(minSdk = UPSIDE_DOWN_CAKE)
  public void getContrast_whenNotSet_returnsDefaultValueOfZero() {
    assertThat(uiModeManager.getContrast()).isEqualTo(0.0f);
  }

  @Test
  @Config(minSdk = UPSIDE_DOWN_CAKE)
  public void setContrast_whenSet_valueIsReturnedByGetContrast() {
    float expectedContrast = -0.16f;

    shadowUiModeManager.setContrast(expectedContrast);

    float actualContrast = uiModeManager.getContrast();
    assertThat(actualContrast).isEqualTo(expectedContrast);
  }

  @Test
  @Config(minSdk = UPSIDE_DOWN_CAKE)
  public void setContrast_whenCalledWithValueAboveOne_throws() {
    assertThrows(IllegalArgumentException.class, () -> shadowUiModeManager.setContrast(1.1f));
  }

  @Test
  @Config(minSdk = UPSIDE_DOWN_CAKE)
  public void setContrast_whenCalledWithValueBelowMinusOne_throws() {
    assertThrows(IllegalArgumentException.class, () -> shadowUiModeManager.setContrast(-1.1f));
  }

  private void setPermissions(String... permissions) {
    PackageInfo pi = new PackageInfo();
    pi.packageName = context.getPackageName();
    pi.versionCode = 1;
    pi.requestedPermissions = permissions;
    shadowOf(context.getPackageManager()).installPackage(pi);
  }
}
