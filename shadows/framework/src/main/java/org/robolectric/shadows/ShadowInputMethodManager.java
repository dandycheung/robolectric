package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.P;
import static android.os.Build.VERSION_CODES.Q;
import static android.os.Build.VERSION_CODES.R;
import static android.os.Build.VERSION_CODES.S;
import static android.os.Build.VERSION_CODES.TIRAMISU;
import static org.robolectric.util.reflector.Reflector.reflector;

import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.SparseArray;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.ClassName;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.util.reflector.Accessor;
import org.robolectric.util.reflector.Direct;
import org.robolectric.util.reflector.ForType;
import org.robolectric.util.reflector.Static;
import org.robolectric.versioning.AndroidVersions.Baklava;
import org.robolectric.versioning.AndroidVersions.U;
import org.robolectric.versioning.AndroidVersions.V;

/** Shadow for InputMethodManager. */
@Implements(InputMethodManager.class)
public class ShadowInputMethodManager {

  /**
   * Handler for receiving soft input visibility changed event.
   *
   * <p>Since Android does not have any API for retrieving soft input status, most application
   * relies on GUI layout changes to detect the soft input change event. Currently, Robolectric are
   * not able to simulate the GUI change when application changes the soft input through {@code
   * InputMethodManager}, this handler can be used by application to simulate GUI change in response
   * of the soft input change.
   */
  public interface SoftInputVisibilityChangeHandler {

    void handleSoftInputVisibilityChange(boolean softInputVisible);
  }

  /** Handler for receiving PrivateCommands. */
  public interface PrivateCommandListener {
    void onPrivateCommand(View view, String action, Bundle data);
  }

  private static boolean softInputVisible;
  private static Optional<SoftInputVisibilityChangeHandler> visibilityChangeHandler =
      Optional.absent();
  private static Optional<PrivateCommandListener> privateCommandListener = Optional.absent();
  private static List<InputMethodInfo> inputMethodInfoList = ImmutableList.of();
  private static List<InputMethodInfo> enabledInputMethodInfoList = ImmutableList.of();
  private static Optional<InputMethodSubtype> inputMethodSubtype = Optional.absent();

  @Implementation
  protected boolean showSoftInput(View view, int flags) {
    return showSoftInput(view, flags, null);
  }

  @Implementation(maxSdk = R)
  protected boolean showSoftInput(View view, int flags, ResultReceiver resultReceiver) {
    setSoftInputVisibility(true);
    return true;
  }

  @Implementation(minSdk = S, maxSdk = TIRAMISU)
  protected boolean showSoftInput(
      View view, int flags, ResultReceiver resultReceiver, int ignoredReason) {
    return showSoftInput(view, flags, resultReceiver);
  }

  @Implementation(minSdk = U.SDK_INT)
  protected boolean showSoftInput(
      View view,
      @ClassName("android.view.inputmethod.ImeTracker$Token") Object statsToken,
      int flags,
      ResultReceiver resultReceiver,
      int reason) {
    return showSoftInput(view, flags, resultReceiver, reason);
  }

  @Implementation(minSdk = S, maxSdk = V.SDK_INT)
  protected boolean hideSoftInputFromWindow(
      IBinder windowToken, int flags, ResultReceiver resultReceiver, int ignoredReason) {
    return hideSoftInputFromWindow(windowToken, flags, resultReceiver);
  }

  @Implementation(minSdk = Baklava.SDK_INT)
  protected boolean hideSoftInputFromWindow(
      IBinder windowToken,
      int flags,
      ResultReceiver resultReceiver,
      int ignoredReason,
      @ClassName("android.view.inputmethod.ImeTracker$Token") Object statsToken) {
    return hideSoftInputFromWindow(windowToken, flags, resultReceiver);
  }

  @Implementation(maxSdk = R)
  protected boolean hideSoftInputFromWindow(IBinder windowToken, int flags) {
    return hideSoftInputFromWindow(windowToken, flags, null);
  }

  @Implementation
  protected boolean hideSoftInputFromWindow(
      IBinder windowToken, int flags, ResultReceiver resultReceiver) {
    int resultCode;
    if (isSoftInputVisible()) {
      setSoftInputVisibility(false);
      resultCode = InputMethodManager.RESULT_HIDDEN;
    } else {
      resultCode = InputMethodManager.RESULT_UNCHANGED_HIDDEN;
    }

    if (resultReceiver != null) {
      resultReceiver.send(resultCode, null);
    }
    return true;
  }

  @Implementation
  protected void toggleSoftInput(int showFlags, int hideFlags) {
    setSoftInputVisibility(!isSoftInputVisible());
  }

  public boolean isSoftInputVisible() {
    return softInputVisible;
  }

  public void setSoftInputVisibilityHandler(
      SoftInputVisibilityChangeHandler visibilityChangeHandler) {
    ShadowInputMethodManager.visibilityChangeHandler = Optional.of(visibilityChangeHandler);
  }

  private void setSoftInputVisibility(boolean visible) {
    if (visible == softInputVisible) {
      return;
    }
    softInputVisible = visible;
    if (visibilityChangeHandler.isPresent()) {
      visibilityChangeHandler.get().handleSoftInputVisibilityChange(softInputVisible);
    }
  }

  /**
   * The framework implementation does a blocking call to system server. This will deadlock on
   * Robolectric, so just stub out the method.
   */
  @Implementation(minSdk = S)
  protected void closeCurrentInput() {}

  /**
   * Returns the list of {@link InputMethodInfo} that are installed.
   *
   * <p>This method differs from Android implementation by allowing the list to be set using {@link
   * #setInputMethodInfoList(List)}.
   */
  @Implementation
  protected List<InputMethodInfo> getInputMethodList() {
    return inputMethodInfoList;
  }

  /**
   * Sets the list of {@link InputMethodInfo} that are marked as installed. See {@link
   * #getInputMethodList()}.
   */
  public void setInputMethodInfoList(List<InputMethodInfo> inputMethodInfoList) {
    ShadowInputMethodManager.inputMethodInfoList = inputMethodInfoList;
  }

  /**
   * Returns the {@link InputMethodSubtype} that is installed.
   *
   * <p>This method differs from Android implementation by allowing the list to be set using {@link
   * #setCurrentInputMethodSubtype(InputMethodSubtype)}.
   */
  @Implementation
  protected InputMethodSubtype getCurrentInputMethodSubtype() {
    return inputMethodSubtype.orNull();
  }

  /**
   * Sets the current {@link InputMethodSubtype} that will be returned by {@link
   * #getCurrentInputMethodSubtype()}.
   */
  public void setCurrentInputMethodSubtype(InputMethodSubtype inputMethodSubtype) {
    ShadowInputMethodManager.inputMethodSubtype = Optional.of(inputMethodSubtype);
  }

  /**
   * Returns the list of {@link InputMethodInfo} that are enabled.
   *
   * <p>This method differs from Android implementation by allowing the list to be set using {@link
   * #setEnabledInputMethodInfoList(List)}.
   */
  @Implementation
  protected List<InputMethodInfo> getEnabledInputMethodList() {
    return enabledInputMethodInfoList;
  }

  /**
   * Sets the list of {@link InputMethodInfo} that are marked as enabled. See {@link
   * #getEnabledInputMethodList()}.
   */
  public void setEnabledInputMethodInfoList(List<InputMethodInfo> inputMethodInfoList) {
    enabledInputMethodInfoList = inputMethodInfoList;
  }

  @Implementation
  protected void restartInput(View view) {}

  @Implementation
  protected boolean isActive(View view) {
    return false;
  }

  @Implementation
  protected boolean isActive() {
    return false;
  }

  @Implementation
  protected boolean isFullscreenMode() {
    return false;
  }

  @Implementation(maxSdk = Q)
  protected void focusIn(View view) {}

  @Implementation(minSdk = M, maxSdk = Q)
  protected void onViewDetachedFromWindow(View view) {}

  @Implementation
  protected void displayCompletions(View view, CompletionInfo[] completions) {}

  @Implementation(maxSdk = LOLLIPOP_MR1)
  protected static InputMethodManager peekInstance() {
    // Android has a bug pre M where peekInstance was dereferenced without a null check:-
    // https://github.com/aosp-mirror/platform_frameworks_base/commit/a046faaf38ad818e6b5e981a39fd7394cf7cee03
    // So for earlier versions, just call through directly to getInstance()
    if (RuntimeEnvironment.getApiLevel() <= LOLLIPOP_MR1) {
      return InputMethodManager.getInstance();
    }
    return reflector(InputMethodManagerReflector.class).peekInstance();
  }

  @Implementation(minSdk = N)
  protected boolean startInputInner(
      int startInputReason,
      IBinder windowGainingFocus,
      int startInputFlags,
      int softInputMode,
      int windowFlags) {
    return true;
  }

  @Implementation(minSdk = M)
  protected void sendAppPrivateCommand(View view, String action, Bundle data) {
    if (privateCommandListener.isPresent()) {
      privateCommandListener.get().onPrivateCommand(view, action, data);
    }
  }

  public void setAppPrivateCommandListener(PrivateCommandListener listener) {
    privateCommandListener = Optional.of(listener);
  }

  @Resetter
  public static void reset() {
    int apiLevel = RuntimeEnvironment.getApiLevel();
    InputMethodManagerReflector inputManagerReflector =
        reflector(InputMethodManagerReflector.class);
    inputManagerReflector.setInstance(null);
    if (apiLevel > P) {
      inputManagerReflector.getInstanceMap().clear();
    }
    softInputVisible = false;
    visibilityChangeHandler = Optional.absent();
    privateCommandListener = Optional.absent();
    inputMethodInfoList = ImmutableList.of();
    enabledInputMethodInfoList = ImmutableList.of();
    inputMethodSubtype = Optional.absent();
  }

  @ForType(InputMethodManager.class)
  interface InputMethodManagerReflector {

    @Static
    @Direct
    InputMethodManager peekInstance();

    @Static
    @Accessor("mInstance")
    void setMInstance(InputMethodManager instance);

    @Static
    @Accessor("sInstance")
    void setInstance(InputMethodManager instance);

    @Static
    @Accessor("sInstanceMap")
    SparseArray<InputMethodManager> getInstanceMap();
  }
}
