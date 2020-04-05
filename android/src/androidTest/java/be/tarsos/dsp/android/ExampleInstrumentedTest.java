package be.tarsos.dsp.android;


import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleInstrumentedTest {
    @Test
    void useAppContext() {
        // Context of the app under test.
        Context appContext  = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("be.tarsos.dsp.android.test", appContext.getPackageName());
    }
}
