package com.google.zetasql;

public class ParseResumeLocationUtils {
    private ParseResumeLocationUtils() {
    }

    public static boolean shouldResume(ParseResumeLocation parseResumeLocation) {
        return parseResumeLocation.getAllowResume() && (parseResumeLocation.getInput().length() > parseResumeLocation.getBytePosition());
    }
}
