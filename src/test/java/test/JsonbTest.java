package test;

import lombok.Data;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;
import javax.json.bind.annotation.JsonbAnnotation;
import javax.json.bind.annotation.JsonbTypeDeserializer;
import javax.json.bind.annotation.JsonbTypeSerializer;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;

@SuppressWarnings("unused")
class JsonbTest {

    static final String CIRCLE_JSON = "{'@type':'circle','radius':5.0}".replace('\'', '"');
    static final String SQUARE_JSON = "{'side':2.0,'@type':'square'}".replace('\'', '"');
    static final String JSON = "[" + CIRCLE_JSON + "," + SQUARE_JSON + "]";
    static final Circle CIRCLE = new Circle().setRadius(5.0);
    static final Square SQUARE = new Square().setSide(2.0);
    static final List<Shape> SHAPES = List.of(CIRCLE, SQUARE);

    static final Jsonb JSONB = JsonbBuilder.create();
    static final Type SHAPE_LIST = new ArrayList<Shape>() {}.getClass().getGenericSuperclass();

    @JsonbAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
    @Repeatable(JsonbTypeAliases.class)
    public @interface JsonbTypeAlias {
        String name();

        Class<?> type();
    }

    @JsonbAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
    public @interface JsonbTypeAliases {
        JsonbTypeAlias[] value();
    }

    public static class PolymorphicDeserializer<T> implements JsonbDeserializer<T> {
        @Override public T deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            JsonObject value = parser.getObject();
            @SuppressWarnings("unchecked") Class<T> superType = (Class<T>) rtType;
            return JSONB.fromJson(value.toString(), classFor(value, superType));
        }

        private Class<? extends T> classFor(JsonObject value, Class<T> superType) {
            String typeString = value.getString("@type", "default");
            for (
                JsonbTypeAlias typeAlias : superType.getAnnotationsByType(JsonbTypeAlias.class)) {
                if (typeAlias.name().equals(typeString))
                    return typeAlias.type().asSubclass(superType);
            }
            throw new JsonbException("unknown shape type " + typeString);
        }
    }

    @JsonbTypeDeserializer(PolymorphicDeserializer.class)
    @JsonbTypeAlias(name = "circle", type = Circle.class)
    @JsonbTypeAlias(name = "square", type = Square.class)
    public interface Shape {}


    public static class CircleSerializer implements JsonbSerializer<Circle> {
        @Override public void serialize(Circle obj, JsonGenerator generator, SerializationContext ctx) {
            generator
                .writeStartObject()
                .write("@type", "circle")
                .write("radius", obj.getRadius())
                .writeEnd();
        }
    }

    @JsonbTypeSerializer(CircleSerializer.class)
    @Accessors(chain = true)
    public static @Data class Circle implements Shape {
        double radius;
    }


    public static class SquareSerializer implements JsonbSerializer<Square> {
        @Override public void serialize(Square obj, JsonGenerator generator, SerializationContext ctx) {
            generator
                .writeStartObject()
                .write("@type", "square")
                .write("side", obj.getSide())
                .writeEnd();
        }
    }

    @JsonbTypeSerializer(SquareSerializer.class)
    @Accessors(chain = true)
    public static @Data class Square implements Shape {
        double side;
    }


    @Test void shouldSerializeList() {
        String json = JSONB.toJson(SHAPES);

        then(JSONB.<List<Shape>>fromJson(json, SHAPE_LIST)).isEqualTo(SHAPES);
    }

    @Test void shouldDeserializeCircle() {
        Circle circle = JSONB.fromJson(CIRCLE_JSON, Circle.class);

        then(circle).isEqualTo(CIRCLE);
    }

    @Test void shouldDeserializeSquare() {
        Square square = JSONB.fromJson(SQUARE_JSON, Square.class);

        then(square).isEqualTo(SQUARE);
    }

    @Test void shouldDeserializeList() {
        List<Shape> shapes = JSONB.fromJson(JSON, SHAPE_LIST);

        then(shapes).isEqualTo(SHAPES);
    }

    @Test void shouldFailToDeserializeMissingTypeField() {
        Throwable throwable = catchThrowable(() ->
            JSONB.fromJson("[{'radius':5.0}]".replace('\'', '"'), SHAPE_LIST)
        );

        then(throwable).isInstanceOf(JsonbException.class)
            .hasMessage("unknown shape type default");
    }

    @Test void shouldFailToDeserializeUnknownTypeField() {
        Throwable throwable = catchThrowable(() ->
            JSONB.fromJson("[{'@type':'cube','side':13.0}]".replace('\'', '"'), SHAPE_LIST)
        );

        then(throwable).isInstanceOf(JsonbException.class)
            .hasMessage("unknown shape type cube");
    }
}
