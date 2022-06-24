/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2021 Lewis D (ItsLewizzz). All rights reserved.
 */

package fun.lewisdev.savedynamicshop.util;

import org.bukkit.ChatColor;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class TextUtil {

    private final static String[] SUFFIX = new String[]{"","k", "M", "B", "T"};
    private final static int MAX_LENGTH = 5;
    private final static DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private final static NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);

    public static String format(double number) {
        String r = new DecimalFormat("##0E0").format(number);
        r = r.replaceAll("E[0-9]", SUFFIX[Character.getNumericValue(r.charAt(r.length() - 1)) / 3]);
        while(r.length() > MAX_LENGTH || r.matches("[0-9]+\\.[a-z]")){
            r = r.substring(0, r.length()-2) + r.substring(r.length() - 1);
        }
        return r;
    }

    public static String numberFormat(double amount) {
        return DECIMAL_FORMAT.format(amount);
    }

    public static String numberFormat(long amount) {
        return NUMBER_FORMAT.format(amount);
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String fromList(List<?> list) {
        if (list == null || list.isEmpty()) return null;
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            if(ChatColor.stripColor(list.get(i).toString()).equals("")) builder.append("\n&r");
            else builder.append(list.get(i).toString()).append(i + 1 != list.size() ? "\n" : "");
        }

        return builder.toString();
    }

    public static String formatTime(long seconds) {
        long sec = seconds % 60;
        long minutes = seconds % 3600 / 60;
        long hours = seconds % 86400 / 3600;
        long days = seconds / 86400;

        if(days > 0) return String.format("%02dd %02dh %02dm %02ds", days, hours, minutes, sec);
        else if(hours > 0) return String.format("%02dh %02dm %02ds", hours, minutes, sec);
        else if(minutes > 0) return String.format("%02dm %02ds", minutes, sec);
        return String.format("%02ds", sec);
    }


}
