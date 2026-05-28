package com.example.lianliankan.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

/**
 * 应用内语言工具。
 *
 * 这个项目没有使用系统级语言配置，而是把语言保存到 SharedPreferences。
 * Activity 创建前必须先包一层带目标 Locale 的 Context，否则切到中文后，
 * 后续页面可能继续拿到旧 Resources，导致“中文切不回英文”。
 */
public final class LocaleUtil {

    public static final String LANGUAGE_EN = "en";
    public static final String LANGUAGE_ZH = "zh";

    private LocaleUtil() {
    }

    public static Context attachBaseContext(Context context) {
        String language = PreferenceUtil.getLanguage(context);
        return updateResources(context, language);
    }

    public static void applyLanguage(Context context, String language) {
        PreferenceUtil.saveLanguage(context, normalizeLanguage(language));
        updateResources(context.getApplicationContext(), language);
    }

    public static Context updateResources(Context context, String language) {
        Locale locale = toLocale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            configuration.setLocales(new android.os.LocaleList(locale));
            return context.createConfigurationContext(configuration);
        } else {
            configuration.setLocale(locale);
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }
    }

    public static Locale toLocale(String language) {
        String safeLanguage = normalizeLanguage(language);
        if (LANGUAGE_ZH.equals(safeLanguage)) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        return Locale.ENGLISH;
    }

    public static String normalizeLanguage(String language) {
        return LANGUAGE_ZH.equals(language) ? LANGUAGE_ZH : LANGUAGE_EN;
    }
}
