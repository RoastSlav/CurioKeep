package org.rostislav.curiokeep.modules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Compares the {@code major.minor.patch[-suffix]} versions used by modules and by the application. */
public final class ModuleVersion {

    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z.-]+))?");

    private ModuleVersion() {
    }

    public static boolean isValid(String version) {
        return version != null && VERSION.matcher(version.trim()).matches();
    }

    /**
     * Negative when {@code a} is older than {@code b}, positive when newer, zero when equal. A version with a suffix (a
     * pre-release) is older than the same numbers without one, as in semantic versioning. Text that is not a version counts
     * as older than any version.
     */
    public static int compare(String a, String b) {
        Matcher left = a == null ? null : VERSION.matcher(a.trim());
        Matcher right = b == null ? null : VERSION.matcher(b.trim());
        boolean leftOk = left != null && left.matches();
        boolean rightOk = right != null && right.matches();
        if (!leftOk || !rightOk) return Boolean.compare(leftOk, rightOk);
        for (int group = 1; group <= 3; group++) {
            int byNumber = Long.compare(Long.parseLong(left.group(group)), Long.parseLong(right.group(group)));
            if (byNumber != 0) return byNumber;
        }
        String leftSuffix = left.group(4);
        String rightSuffix = right.group(4);
        if (leftSuffix == null || rightSuffix == null) return Boolean.compare(leftSuffix == null, rightSuffix == null);
        return leftSuffix.compareTo(rightSuffix);
    }
}
