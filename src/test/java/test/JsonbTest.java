package test;

import lombok.Data;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;
import javax.json.bind.annotation.JsonbTypeDeserializer;
import javax.json.bind.annotation.JsonbTypeSerializer;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
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


    public static class ShapeDeserializer implements JsonbDeserializer<Shape> {
        @Override public Shape deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            JsonObject value = parser.getObject();
            String type = value.getString("@type", "<null>");
            return JSONB.fromJson(value.toString(), classFor(type));
        }

        private Class<? extends Shape> classFor(String type) {
            switch (type) {
                case "circle":
                    return Circle.class;
                case "square":
                    return Square.class;
                default:
                    throw new JsonbException("unknown shape type " + type);
            }
        }
    }

    @JsonbTypeDeserializer(ShapeDeserializer.class)
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
            .hasMessage("unknown shape type <null>");
    }

    @Test void shouldFailToDeserializeUnknownTypeField() {
        Throwable throwable = catchThrowable(() ->
            JSONB.fromJson("[{'@type':'cube','side':13.0}]".replace('\'', '"'), SHAPE_LIST)
        );

        then(throwable).isInstanceOf(JsonbException.class)
            .hasMessage("unknown shape type cube");
    }
}
