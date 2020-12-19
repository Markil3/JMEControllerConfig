package markil3.controller;

import android.content.res.Configuration;
import android.os.Bundle;

import com.jme3.app.AndroidHarness;
import com.jme3.system.AppSettings;

import java.util.logging.Level;
import java.util.logging.LogManager;

public class MainActivity extends AndroidHarness
{
    /*
     * Note that you can ignore the errors displayed in this file, the android
     * project will build regardless. Install the 'Android' plugin under
     * Tools->Plugins->Available Plugins to get error checks and code completion
     * for the Android project files.
     */
    public MainActivity()
    {
        /*
         * Set the default logging level (default=Level.INFO, Level.ALL=All
         * Debug Info)
         */
        LogManager.getLogManager().getLogger("").setLevel(Level.INFO);

        this.appClass = Main.class.getName();

        // Set the desired EGL configuration
        eglBitsPerPixel = 24;
        eglAlphaBits = 0;
        eglDepthBits = 16;
        eglSamples = 0;
        eglStencilBits = 0;

        // Set the maximum framerate
        // (default = -1 for unlimited)
        frameRate = -1;

        // Set the maximum resolution dimension
        // (the smaller side, height or width, is set automatically
        // to maintain the original device screen aspect ratio)
        // (default = -1 to match device screen resolution)
//        maxResolutionDimension = -1;

        // Set input configuration settings
        joystickEventsEnabled = true;
        keyEventsEnabled = true;
        mouseEventsEnabled = true;

        // Set application exit settings
        finishOnAppStop = true;
        handleExitHook = false;
        exitDialogTitle = "Do you want to exit?";
        exitDialogMessage =
                "Use your home key to bring this app into the background " +
                        "or exit to terminate it.";

        // Set splash screen resource id, if used
        // (default = 0, no splash screen)
        // For example, if the image file name is "splash"...
        //     splashPicID = R.drawable.splash;
        splashPicID = 0;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setImmersive(true);
    }
}
