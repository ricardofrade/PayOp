package com.rfrade.payop.presentation.util;

import java.util.Locale;

public class CurrencyFormatter {
    public static String formatCents(long cents) {
        return String.format(Locale.getDefault(), "$%,.2f", cents / 100.0);
    }
}
