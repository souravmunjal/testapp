package apps.startup.test

import android.app.Activity
import android.view.View
import com.google.android.material.snackbar.Snackbar

public class Toaster {
    companion object {
        fun makeSnackBarFromActivity(activity: Activity?, message: String) {
            if (activity != null) {
                val snackbar = Snackbar.make(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                snackbar.setAction("Dismiss") { snackbar.dismiss() }
                snackbar.show()
            }
        }

        fun makeSnackBarFromView(view: View?, message: String) {
            if (view != null) {
                val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                snackbar.setAction("Dismiss") { snackbar.dismiss() }
                snackbar.show()
            }
        }
    }



}