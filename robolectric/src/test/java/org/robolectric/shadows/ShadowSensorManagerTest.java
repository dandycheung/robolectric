package org.robolectric.shadows;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_ALL;
import static android.hardware.Sensor.TYPE_GYROSCOPE;
import static android.os.Build.VERSION_CODES.O;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorDirectChannel;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Looper;
import android.os.MemoryFile;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.junit.rules.SetSystemPropertyRule;

@RunWith(AndroidJUnit4.class)
public class ShadowSensorManagerTest {
  @Rule public SetSystemPropertyRule setSystemPropertyRule = new SetSystemPropertyRule();

  private SensorManager sensorManager;
  private ShadowSensorManager shadow;

  @Before
  public void setUp() {
    sensorManager =
        (SensorManager)
            ApplicationProvider.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
    shadow = shadowOf(sensorManager);
  }

  @After
  public void tearDown() {
    sensorManager = null;
    shadow = null;
  }

  @Test
  @Config(minSdk = Build.VERSION_CODES.O)
  public void createDirectChannel() throws Exception {
    SensorDirectChannel channel = sensorManager.createDirectChannel(new MemoryFile("name", 10));
    assertThat(channel.isOpen()).isTrue();

    channel.close();
    assertThat(channel.isOpen()).isFalse();
  }

  @Test
  public void shouldReturnHasListenerAfterRegisteringListener() {
    SensorEventListener listener = registerListener();

    assertThat(shadow.hasListener(listener)).isTrue();
  }

  private SensorEventListener registerListener() {
    SensorEventListener listener = new TestSensorEventListener();
    Sensor sensor = sensorManager.getDefaultSensor(SensorManager.SENSOR_ACCELEROMETER);
    sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);

    return listener;
  }

  @Test
  public void shouldReturnHasNoListenerAfterUnregisterListener() {
    SensorEventListener listener = registerListener();
    sensorManager.unregisterListener(
        listener, sensorManager.getDefaultSensor(SensorManager.SENSOR_ACCELEROMETER));

    assertThat(shadow.hasListener(listener)).isFalse();
  }

  @Test
  public void shouldReturnHasNoListenerAfterUnregisterListenerWithoutSpecificSensor() {
    SensorEventListener listener = registerListener();
    sensorManager.unregisterListener(listener);

    assertThat(shadow.hasListener(listener)).isFalse();
  }

  @Test
  public void shouldReturnHasNoListenerByDefault() {
    SensorEventListener listener = new TestSensorEventListener();

    assertThat(shadow.hasListener(listener)).isFalse();
  }

  @Test
  public void shouldTrackSingleListenerRegistrationForDifferentSensors() {
    SensorEventListener listener = new TestSensorEventListener();
    Sensor accelSensor = ShadowSensor.newInstance(TYPE_ACCELEROMETER);
    Sensor gyroSensor = ShadowSensor.newInstance(TYPE_GYROSCOPE);

    sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
    sensorManager.registerListener(listener, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);

    assertThat(shadow.hasListener(listener)).isTrue();
    assertThat(shadow.hasListener(listener, accelSensor)).isTrue();
    assertThat(shadow.hasListener(listener, gyroSensor)).isTrue();

    sensorManager.unregisterListener(listener, accelSensor);
    assertThat(shadow.hasListener(listener)).isTrue();
    assertThat(shadow.hasListener(listener, accelSensor)).isFalse();
    assertThat(shadow.hasListener(listener, gyroSensor)).isTrue();

    sensorManager.unregisterListener(listener, gyroSensor);
    assertThat(shadow.hasListener(listener)).isFalse();
    assertThat(shadow.hasListener(listener, accelSensor)).isFalse();
    assertThat(shadow.hasListener(listener, gyroSensor)).isFalse();
  }

  @Test
  public void shouldSendSensorEventToSingleRegisteredListener() {
    TestSensorEventListener listener = new TestSensorEventListener();
    Sensor sensor = sensorManager.getDefaultSensor(SensorManager.SENSOR_ACCELEROMETER);
    sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    SensorEvent event = shadow.createSensorEvent();
    // Confirm that the listener has received no events yet.
    assertThat(listener.getLatestSensorEvent()).isAbsent();

    shadow.sendSensorEventToListeners(event);

    Optional<SensorEvent> latestSensorEvent = listener.getLatestSensorEvent();
    assertThat(latestSensorEvent).isPresent();
    assertThat(latestSensorEvent).hasValue(event);
  }

  @Test
  public void shouldSendSensorEventToMultipleRegisteredListeners() {
    TestSensorEventListener listener1 = new TestSensorEventListener();
    TestSensorEventListener listener2 = new TestSensorEventListener();
    Sensor sensor = sensorManager.getDefaultSensor(SensorManager.SENSOR_ACCELEROMETER);
    sensorManager.registerListener(listener1, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    sensorManager.registerListener(listener2, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    SensorEvent event = shadow.createSensorEvent();

    shadow.sendSensorEventToListeners(event);

    Optional<SensorEvent> latestSensorEvent1 = listener1.getLatestSensorEvent();
    assertThat(latestSensorEvent1).isPresent();
    assertThat(latestSensorEvent1).hasValue(event);

    Optional<SensorEvent> latestSensorEvent2 = listener2.getLatestSensorEvent();
    assertThat(latestSensorEvent2).isPresent();
    assertThat(latestSensorEvent2).hasValue(event);
  }

  @Test
  public void shouldNotCauseConcurrentModificationExceptionSendSensorEvent() {
    TestSensorEventListener listener1 =
        new TestSensorEventListener() {
          @Override
          public void onSensorChanged(SensorEvent event) {
            super.onSensorChanged(event);
            sensorManager.unregisterListener(this);
          }
        };
    Sensor sensor = sensorManager.getDefaultSensor(SensorManager.SENSOR_ACCELEROMETER);
    sensorManager.registerListener(listener1, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    SensorEvent event = shadow.createSensorEvent();

    shadow.sendSensorEventToListeners(event);

    assertThat(listener1.getLatestSensorEvent()).hasValue(event);
  }

  @Test
  public void shouldNotSendSensorEventIfNoRegisteredListeners() {
    // Create a listener but don't register it.
    TestSensorEventListener listener = new TestSensorEventListener();
    SensorEvent event = shadow.createSensorEvent();

    shadow.sendSensorEventToListeners(event);

    assertThat(listener.getLatestSensorEvent()).isAbsent();
  }

  @Test
  public void shouldCreateSensorEvent() {
    assertThat(shadow.createSensorEvent()).isNotNull();
  }

  @Test
  public void shouldCreateSensorEventWithValueArray() {
    SensorEvent event = ShadowSensorManager.createSensorEvent(3);
    assertThat(event.values.length).isEqualTo(3);
  }

  @Test
  public void shouldCreateSensorEventWithValueArrayAndSensorType() {
    SensorEvent event = ShadowSensorManager.createSensorEvent(3, Sensor.TYPE_GRAVITY);
    assertThat(event.values.length).isEqualTo(3);
    assertThat(event.sensor).isNotNull();
    assertThat(event.sensor.getType()).isEqualTo(Sensor.TYPE_GRAVITY);
  }

  @Test
  public void createSensorEvent_shouldThrowExceptionWhenValueLessThan1() {
    try {
      ShadowSensorManager.createSensorEvent(/* valueArraySize= */ 0);
      fail("Expected IllegalArgumentException not thrown");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void getSensor_shouldBeConfigurable() {
    Sensor sensor = ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER);
    shadowOf(sensorManager).addSensor(sensor);
    assertThat(sensor).isSameInstanceAs(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
  }

  @Test
  public void removeSensor_shouldRemoveAddedSensor() {
    Sensor sensor = ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER);
    ShadowSensorManager shadowSensorManager = shadowOf(sensorManager);
    shadowSensorManager.addSensor(sensor);
    assertThat(sensor).isSameInstanceAs(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    shadowSensorManager.removeSensor(sensor);
    assertThat(sensorManager.getDefaultSensor(TYPE_ACCELEROMETER)).isNull();
  }

  @Test
  public void shouldReturnEmptySensorListIfNoneCreated() {
    assertThat(sensorManager.getSensorList(0)).isEmpty();
  }

  @Test
  public void shouldReturnAllRelevantSensorsForGivenType() {
    ShadowSensorManager shadowSensorManager = shadowOf(sensorManager);
    Sensor gyroscopeSensor1 = ShadowSensor.newInstance(TYPE_GYROSCOPE);
    Sensor gyroscopeSensor2 = ShadowSensor.newInstance(TYPE_GYROSCOPE);
    Sensor irrelevantAccelSensor1 = ShadowSensor.newInstance(TYPE_ACCELEROMETER);

    shadowSensorManager.addSensor(gyroscopeSensor1);
    shadowSensorManager.addSensor(gyroscopeSensor2);
    shadowSensorManager.addSensor(irrelevantAccelSensor1);
    List<Sensor> allGyroSensors = sensorManager.getSensorList(TYPE_GYROSCOPE);

    assertThat(allGyroSensors).containsExactly(gyroscopeSensor1, gyroscopeSensor2);
  }

  @Test
  public void shouldReturnAllSensorsInAList() {
    List<Sensor> multipleShadowSensors = new ArrayList<>();
    multipleShadowSensors.add(ShadowSensor.newInstance(TYPE_ACCELEROMETER));
    multipleShadowSensors.add(ShadowSensor.newInstance(TYPE_GYROSCOPE));
    shadow.addSensor(multipleShadowSensors.get(0));
    shadow.addSensor(multipleShadowSensors.get(1));

    List<Sensor> allRetrievedSensors = sensorManager.getSensorList(TYPE_ALL);

    assertThat(allRetrievedSensors).containsExactlyElementsIn(multipleShadowSensors);
  }

  @Test
  public void flush_shouldCallOnFlushCompleted() {
    Sensor accelSensor = ShadowSensor.newInstance(TYPE_ACCELEROMETER);
    Sensor gyroSensor = ShadowSensor.newInstance(TYPE_GYROSCOPE);

    TestSensorEventListener listener1 = new TestSensorEventListener();
    TestSensorEventListener listener2 = new TestSensorEventListener();
    TestSensorEventListener listener3 = new TestSensorEventListener();

    sensorManager.registerListener(listener1, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
    sensorManager.registerListener(listener2, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
    sensorManager.registerListener(listener2, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);

    // Call flush with the first listener. It should return true (as the flush
    // succeeded), and should call onFlushCompleted for all listeners registered for accelSensor.
    assertThat(sensorManager.flush(listener1)).isTrue();
    shadowOf(Looper.getMainLooper()).idle();

    assertThat(listener1.getOnFlushCompletedCalls()).containsExactly(accelSensor);
    assertThat(listener2.getOnFlushCompletedCalls()).containsExactly(accelSensor);
    assertThat(listener3.getOnFlushCompletedCalls()).isEmpty();

    // Call flush with the second listener. It should again return true, and should call
    // onFlushCompleted for all listeners registered for accelSensor and gyroSensor.
    assertThat(sensorManager.flush(listener2)).isTrue();
    shadowOf(Looper.getMainLooper()).idle();

    // From the two calls to flush, onFlushCompleted should have been called twice for accelSensor
    // and once for gyroSensor.
    assertThat(listener1.getOnFlushCompletedCalls()).containsExactly(accelSensor, accelSensor);
    assertThat(listener2.getOnFlushCompletedCalls())
        .containsExactly(accelSensor, accelSensor, gyroSensor);
    assertThat(listener3.getOnFlushCompletedCalls()).isEmpty();

    // Call flush with the third listener. This listener is not registered for any sensors, so it
    // should return false.
    assertThat(sensorManager.flush(listener3)).isFalse();
    shadowOf(Looper.getMainLooper()).idle();

    // There should not have been any more onFlushCompleted calls.
    assertThat(listener1.getOnFlushCompletedCalls()).containsExactly(accelSensor, accelSensor);
    assertThat(listener2.getOnFlushCompletedCalls())
        .containsExactly(accelSensor, accelSensor, gyroSensor);
    assertThat(listener3.getOnFlushCompletedCalls()).isEmpty();
  }

  @Test
  public void getDefaultSensorWithNoSensorAdded_shouldReturnNull() {
    assertThat(sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)).isNull();
    assertThat(sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE, true)).isNull();
    assertThat(sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE, false)).isNull();
    assertThat(sensorManager.getSensorList(Sensor.TYPE_HEART_RATE)).isEmpty();
  }

  @Test
  public void getDefaultSensorWithWakeUpSensor_shouldReturnCorrectSensorAndWakeUpValue() {
    Sensor sensor = ShadowSensor.newInstance(Sensor.TYPE_HEART_RATE);
    shadowOf(sensor).setWakeUpFlag(true);
    shadowOf(sensorManager).addSensor(sensor);

    assertThat(sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)).isEqualTo(sensor);
    assertThat(sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE, true)).isEqualTo(sensor);
    assertThat(sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE, false)).isNull();
  }

  @Test
  public void getDefaultSensorWithNoWakeUpSensor_shouldReturnCorrectSensorAndWakeUpValue() {
    Sensor sensor = ShadowSensor.newInstance(Sensor.TYPE_HEART_RATE);
    shadowOf(sensor).setWakeUpFlag(false);
    shadowOf(sensorManager).addSensor(sensor);

    assertThat(sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)).isEqualTo(sensor);
    assertThat(sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE, false)).isEqualTo(sensor);
    assertThat(sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE, true)).isNull();
  }

  private static class TestSensorEventListener implements SensorEventListener2 {
    private Optional<SensorEvent> latestSensorEvent = Optional.absent();
    private final List<Sensor> onFlushCompletedCalls = new ArrayList<>();

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onSensorChanged(SensorEvent event) {
      latestSensorEvent = Optional.of(event);
    }

    @Override
    public void onFlushCompleted(Sensor sensor) {
      onFlushCompletedCalls.add(sensor);
    }

    public List<Sensor> getOnFlushCompletedCalls() {
      return onFlushCompletedCalls;
    }

    public Optional<SensorEvent> getLatestSensorEvent() {
      return latestSensorEvent;
    }
  }

  @Test
  @Config(minSdk = O)
  public void sensorManager_activityContextEnabled_retrievesSameSensors() {
    setSystemPropertyRule.set("robolectric.createActivityContexts", "true");

    try (ActivityController<Activity> controller =
        Robolectric.buildActivity(Activity.class).setup()) {
      SensorManager applicationSensorManager =
          (SensorManager)
              ApplicationProvider.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
      Activity activity = controller.get();
      SensorManager activitySensorManager =
          (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);

      assertThat(applicationSensorManager).isNotSameInstanceAs(activitySensorManager);

      List<Sensor> applicationSensors = applicationSensorManager.getSensorList(Sensor.TYPE_ALL);
      List<Sensor> activitySensors = activitySensorManager.getSensorList(Sensor.TYPE_ALL);

      assertThat(activitySensors).hasSize(applicationSensors.size());

      for (int i = 0; i < applicationSensors.size(); i++) {
        Sensor appSensor = applicationSensors.get(i);
        Sensor actSensor = activitySensors.get(i);

        assertThat(appSensor.getName()).isEqualTo(actSensor.getName());
        assertThat(appSensor.getType()).isEqualTo(actSensor.getType());
        assertThat(appSensor.getMaximumRange()).isEqualTo(actSensor.getMaximumRange());
        assertThat(appSensor.getResolution()).isEqualTo(actSensor.getResolution());
        assertThat(appSensor.getPower()).isEqualTo(actSensor.getPower());
        assertThat(appSensor.getMinDelay()).isEqualTo(actSensor.getMinDelay());
      }
    }
  }
}
