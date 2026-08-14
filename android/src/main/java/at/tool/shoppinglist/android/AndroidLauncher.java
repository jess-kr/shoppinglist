package at.tool.shoppinglist.android;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import at.tool.shoppinglist.Main;

/** Launches the Android application. */
public class AndroidLauncher extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        configuration.useImmersiveMode = false;
        Main main = new Main();
        main.setDatabase(new AndroidDatabase(this));
        View gameView = initializeForView(main, configuration);

        // Wrap the game view and apply padding to the wrapper.
        // This is required on Android 15+ to respect system bars.
        FrameLayout wrapper = new FrameLayout(this);
        wrapper.addView(gameView);

        ViewCompat.setOnApplyWindowInsetsListener(wrapper, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(0, top, 0, bottom);
            return insets;
        });

        setContentView(wrapper);
    }
}
