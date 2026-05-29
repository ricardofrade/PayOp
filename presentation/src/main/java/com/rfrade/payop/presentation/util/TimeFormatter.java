package com.rfrade.payop.presentation.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeFormatter {
    private static final ThreadLocal<SimpleDateFormat> FORMATTER =
            new ThreadLocal<SimpleDateFormat>() {
                @Override
                protected SimpleDateFormat initialValue() {
                    return new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                }
            };

    public static String format(long timestamp) {
        return FORMATTER.get().format(new Date(timestamp));
    }
}
