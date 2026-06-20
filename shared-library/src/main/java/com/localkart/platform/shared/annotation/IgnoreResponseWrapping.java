package com.localkart.platform.shared.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to bypass the automatic ResponseBodyAdvice wrapping of REST responses
 * in the standard {@link com.localkart.platform.shared.model.ApiResponse}.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreResponseWrapping {
}
