package com.github.t1.polyjsonb;

import javax.json.bind.annotation.JsonbAnnotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@JsonbAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
public @interface JsonbTypeAliases {
    JsonbTypeAlias[] value();
}
