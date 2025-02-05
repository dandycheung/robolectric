package org.robolectric;

import com.example.objects.OuterDummy.InnerDummy;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import org.robolectric.annotation.processing.shadows.ShadowInnerDummyWithPicker;
import org.robolectric.internal.ShadowProvider;
import org.robolectric.shadow.api.Shadow;

/** Shadow mapper. Automatically generated by the Robolectric Annotation Processor. */
@Generated("org.robolectric.annotation.processing.RobolectricProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public class Shadows implements ShadowProvider {
  private static final List<Map.Entry<String, String>> SHADOWS = new ArrayList<>(3);

  static {
    SHADOWS.add(
        new AbstractMap.SimpleImmutableEntry<>(
            "com.example.objects.OuterDummy.InnerDummy",
            "org.robolectric.annotation.processing.shadows.ShadowInnerDummyWithPicker$ShadowInnerDummyWithPicker2"));
    SHADOWS.add(
        new AbstractMap.SimpleImmutableEntry<>(
            "com.example.objects.OuterDummy.InnerDummy2",
            "org.robolectric.annotation.processing.shadows.ShadowInnerDummyWithPicker$ShadowInnerDummyWithPicker3"));
  }

  public static ShadowInnerDummyWithPicker shadowOf(InnerDummy actual) {
    return Shadow.extract(actual);
  }

  @Override
  public void reset() {}

  @Override
  public Collection<Map.Entry<String, String>> getShadows() {
    return SHADOWS;
  }

  @Override
  public String[] getProvidedPackageNames() {
    return new String[] {"com.example.objects"};
  }

  private static final Map<String, String> SHADOW_PICKER_MAP = new HashMap<>(12);

  static {
    SHADOW_PICKER_MAP.put(
        "com.example.objects.OuterDummy$InnerDummy",
        "org.robolectric.annotation.processing.shadows.ShadowInnerDummyWithPicker$Picker");
    SHADOW_PICKER_MAP.put(
        "com.example.objects.OuterDummy$InnerDummy",
        "org.robolectric.annotation.processing.shadows.ShadowInnerDummyWithPicker$Picker");
    SHADOW_PICKER_MAP.put(
        "com.example.objects.OuterDummy$InnerDummy2",
        "org.robolectric.annotation.processing.shadows.ShadowInnerDummyWithPicker$Picker");
  }

  @Override
  public Map<String, String> getShadowPickerMap() {
    return SHADOW_PICKER_MAP;
  }
}