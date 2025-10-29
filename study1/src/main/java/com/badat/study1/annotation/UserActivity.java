package com.badat.study1.annotation;

import com.badat.study1.model.UserActivityLog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UserActivity {
    String action();
    UserActivityLog.Category category();
}
















