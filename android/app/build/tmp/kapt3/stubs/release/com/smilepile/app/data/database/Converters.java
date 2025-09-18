package com.smilepile.app.data.database;

/**
 * Room type converters for SmilePile database.
 * Handles conversion between complex types and primitive types that Room can persist.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010 \n\u0002\b\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0019\u0010\u0003\u001a\u0004\u0018\u00010\u00042\b\u0010\u0005\u001a\u0004\u0018\u00010\u0006H\u0007\u00a2\u0006\u0002\u0010\u0007J\u001a\u0010\b\u001a\u0004\u0018\u00010\t2\u000e\u0010\n\u001a\n\u0012\u0004\u0012\u00020\t\u0018\u00010\u000bH\u0007J\u0019\u0010\f\u001a\u0004\u0018\u00010\u00062\b\u0010\n\u001a\u0004\u0018\u00010\u0004H\u0007\u00a2\u0006\u0002\u0010\rJ\u001a\u0010\u000e\u001a\n\u0012\u0004\u0012\u00020\t\u0018\u00010\u000b2\b\u0010\n\u001a\u0004\u0018\u00010\tH\u0007\u00a8\u0006\u000f"}, d2 = {"Lcom/smilepile/app/data/database/Converters;", "", "()V", "dateToTimestamp", "", "date", "Ljava/util/Date;", "(Ljava/util/Date;)Ljava/lang/Long;", "fromStringList", "", "value", "", "fromTimestamp", "(Ljava/lang/Long;)Ljava/util/Date;", "toStringList", "app_release"})
public final class Converters {
    
    public Converters() {
        super();
    }
    
    @androidx.room.TypeConverter
    @org.jetbrains.annotations.Nullable
    public final java.util.Date fromTimestamp(@org.jetbrains.annotations.Nullable
    java.lang.Long value) {
        return null;
    }
    
    @androidx.room.TypeConverter
    @org.jetbrains.annotations.Nullable
    public final java.lang.Long dateToTimestamp(@org.jetbrains.annotations.Nullable
    java.util.Date date) {
        return null;
    }
    
    @androidx.room.TypeConverter
    @org.jetbrains.annotations.Nullable
    public final java.lang.String fromStringList(@org.jetbrains.annotations.Nullable
    java.util.List<java.lang.String> value) {
        return null;
    }
    
    @androidx.room.TypeConverter
    @org.jetbrains.annotations.Nullable
    public final java.util.List<java.lang.String> toStringList(@org.jetbrains.annotations.Nullable
    java.lang.String value) {
        return null;
    }
}