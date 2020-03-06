package com.github.t1.polyjsonb;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.stream.JsonParser;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

public class PolymorphicDeserializer<T> implements JsonbDeserializer<T> {
    private static final Jsonb JSONB = JsonbBuilder.create();

    @Override public T deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
        JsonObject value = parser.getObject();
        @SuppressWarnings("unchecked") Class<T> superType = (Class<T>) rtType;
        Class<? extends T> subType = new SubTypeFinder<>(value, superType).find();
        return JSONB.fromJson(value.toString(), subType);
    }

    private static class SubTypeFinder<T> {
        private final JsonObject value;
        private final Class<T> superType;
        private final String typeFieldValue;

        public SubTypeFinder(JsonObject value, Class<T> superType) {
            this.value = value;
            this.superType = superType;
            this.typeFieldValue = value.getString("type", "default");
        }

        public Class<? extends T> find() {
            for (JsonbTypeAlias typeAlias : superType.getAnnotationsByType(JsonbTypeAlias.class)) {
                if (matches(typeAlias.name())) {
                    return typeAlias.type().asSubclass(superType);
                }
            }
            throw new JsonbException("unknown shape type '" + typeFieldValue + "'");
        }

        private boolean matches(String typeAliasName) {
            if (typeAliasName.startsWith("{") && typeAliasName.endsWith("}")) {
                String methodName = typeAliasName.substring(1, typeAliasName.length() - 1);
                return matchesPredicateMethod(methodName);
            } else {
                return typeAliasName.equals(typeFieldValue);
            }
        }

        private boolean matchesPredicateMethod(String methodName) {
            try {
                Method method = superType.getMethod(methodName, JsonObject.class);
                if (!Modifier.isStatic(method.getModifiers())) {
                    throw new JsonbException("predicate method '" + methodName + "' is not static");
                }
                if (!method.getReturnType().equals(Boolean.class) && !method.getReturnType().equals(boolean.class)) {
                    throw new JsonbException("predicate method '" + methodName + "' doesn't return a boolean");
                }
                Object result = method.invoke(null, value);
                if (result == null) {
                    throw new JsonbException("predicate method '" + methodName + "' returned null");
                }
                return (boolean) result;
            } catch (NoSuchMethodException e) {
                throw new JsonbException("can't find predicate method '" + methodName + "'");
            } catch (IllegalAccessException e) {
                throw new JsonbException("can't access predicate method '" + methodName + "'");
            } catch (InvocationTargetException e) {
                throw new JsonbException("can't invoke predicate method '" + methodName + "'");
            }
        }
    }
}
