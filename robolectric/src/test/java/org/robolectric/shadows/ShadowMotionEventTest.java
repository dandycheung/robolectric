package org.robolectric.shadows;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.os.Build;
import android.view.MotionEvent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ShadowMotionEventTest {
  private MotionEvent event;
  private ShadowMotionEvent shadowMotionEvent;

  @Before
  public void setUp() throws Exception {
    event = MotionEvent.obtain(100, 200, MotionEvent.ACTION_MOVE, 5.0f, 10.0f, 0);
    shadowMotionEvent = shadowOf(event);
  }

  @Test
  public void addingSecondPointerSetsCount() {
    assertThat(event.getX(0)).isEqualTo(5.0f);
    assertThat(event.getY(0)).isEqualTo(10.0f);
    assertThat(event.getPointerCount()).isEqualTo(1);

    shadowMotionEvent.setPointer2(20.0f, 30.0f);

    assertThat(event.getX(1)).isEqualTo(20.0f);
    assertThat(event.getY(1)).isEqualTo(30.0f);
    assertThat(event.getPointerCount()).isEqualTo(2);
  }

  @Test
  public void canSetPointerIdsByIndex() {
    shadowMotionEvent.setPointer2(20.0f, 30.0f);
    shadowMotionEvent.setPointerIds(2, 5);
    assertEquals(2, event.getPointerId(0));
    assertEquals(5, event.getPointerId(1));
  }

  @Test
  public void indexShowsUpInAction() {
    shadowMotionEvent.setPointerIndex(1);
    assertEquals(
        1 << MotionEvent.ACTION_POINTER_ID_SHIFT | MotionEvent.ACTION_MOVE, event.getAction());
  }

  @Test
  public void canGetActionIndex() {
    assertEquals(0, event.getActionIndex());
    shadowMotionEvent.setPointerIndex(1);
    assertEquals(1, event.getActionIndex());
  }

  @Test
  public void getActionMaskedStripsPointerIndexFromAction() {
    assertEquals(MotionEvent.ACTION_MOVE, event.getActionMasked());
    shadowMotionEvent.setPointerIndex(1);
    assertEquals(MotionEvent.ACTION_MOVE, event.getActionMasked());
  }

  @Test
  public void canFindPointerIndexFromId() {
    shadowMotionEvent.setPointer2(20.0f, 30.0f);
    shadowMotionEvent.setPointerIds(2, 1);
    assertEquals(0, event.findPointerIndex(2));
    assertEquals(1, event.findPointerIndex(1));
    assertEquals(-1, event.findPointerIndex(3));
  }

  @Test
  public void obtainEventsWithDistinctPointerIds() {
    int[] event1Ids = {88};
    MotionEvent.PointerCoords[] event1Coords = {createCoords(5.0f, 10.0f)};
    MotionEvent event1 =
        MotionEvent.obtain(
            0, 0, MotionEvent.ACTION_DOWN, 1, event1Ids, event1Coords, 0, 1.0f, 1.0f, 0, 0, 0, 0);

    int[] event2Ids = {99};
    MotionEvent.PointerCoords[] event2Coords = {createCoords(20.0f, 30.0f)};
    MotionEvent event2 =
        MotionEvent.obtain(
            0, 0, MotionEvent.ACTION_DOWN, 1, event2Ids, event2Coords, 0, 1.0f, 1.0f, 0, 0, 0, 0);

    assertEquals(1, event1.getPointerCount());
    assertEquals(88, event1.getPointerId(0));
    assertEquals(1, event2.getPointerCount());
    assertEquals(99, event2.getPointerId(0));
  }

  private static MotionEvent.PointerCoords createCoords(float x, float y) {
    MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
    coords.x = x;
    coords.y = y;
    return coords;
  }

  @Test
  public void scale() {
    MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
    coords.setAxisValue(MotionEvent.AXIS_X, 10.0f);
    coords.setAxisValue(MotionEvent.AXIS_Y, 20.0f);
    coords.setAxisValue(MotionEvent.AXIS_RELATIVE_X, 30.0f);
    coords.setAxisValue(MotionEvent.AXIS_RELATIVE_Y, 40.0f);
    MotionEvent event = createMotionEventForPointerCoords(coords);
    event.scale(2.0f);
    assertEquals(20.0f, event.getX(0), 0);
    assertEquals(40.0f, event.getY(0), 0);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
      assertEquals(60.0f, event.getAxisValue(MotionEvent.AXIS_RELATIVE_X), 0);
      assertEquals(80.0f, event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y), 0);
    } else {
      assertEquals(30.0f, event.getAxisValue(MotionEvent.AXIS_RELATIVE_X), 0);
      assertEquals(40.0f, event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y), 0);
    }
  }

  private static MotionEvent createMotionEventForPointerCoords(MotionEvent.PointerCoords coords) {
    return MotionEvent.obtain(
        /* downTime= */ 0,
        /* eventTime= */ 0,
        /* action= */ MotionEvent.ACTION_DOWN,
        /* pointerCount= */ 1,
        new int[] {1},
        new MotionEvent.PointerCoords[] {coords},
        /* metaState= */ 0,
        /* xPrecision= */ 0,
        /* yPrecision= */ 0,
        /* deviceId= */ 0,
        /* edgeFlags= */ 0,
        /* source= */ 0,
        /* flags= */ 0);
  }
}
