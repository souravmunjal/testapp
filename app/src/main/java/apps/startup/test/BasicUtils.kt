package apps.startup.test

import java.util.regex.Pattern

public class BasicUtils {
    companion object {
        fun isValidMobile(phone: String): Boolean {
            var check = false
            if (!Pattern.matches("[a-zA-Z]+", phone)) {
                check = !(phone.length < 10 || phone.length > 12)
            } else {
                check = false
            }
            return check
        }
    }

}

