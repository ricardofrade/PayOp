package com.rfrade.payop.data.local;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rfrade.payop.domain.model.StateTransition;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class Converters {

    private static final Gson GSON = new Gson();
    private static final Type LOG_TYPE = new TypeToken<List<StateTransition>>() {
    }.getType();

    @TypeConverter
    public static String fromTransitionLog(List<StateTransition> log) {
        return log == null ? "[]" : GSON.toJson(log);
    }

    @TypeConverter
    public static List<StateTransition> toTransitionLog(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        List<StateTransition> result = GSON.fromJson(json, LOG_TYPE);
        return result != null ? result : Collections.<StateTransition>emptyList();
    }
}
