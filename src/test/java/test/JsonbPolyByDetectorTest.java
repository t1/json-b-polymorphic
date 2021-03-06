package test;

import com.github.t1.polyjsonb.JsonbTypeAlias;
import com.github.t1.polyjsonb.PolymorphicDeserializer;
import lombok.Data;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;
import javax.json.bind.annotation.JsonbTypeDeserializer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;

class JsonbPolyByDetectorTest {

    static final String CIRCLE_JSON = "{\"radius\":5.0}";
    static final String SQUARE_JSON = "{\"side\":2.0}";
    static final String JSON = "[" + CIRCLE_JSON + "," + SQUARE_JSON + "]";
    static final Circle CIRCLE = new Circle().setRadius(5.0);
    static final Square SQUARE = new Square().setSide(2.0);
    static final List<Shape> SHAPES = asList(CIRCLE, SQUARE);

    static final Jsonb JSONB = JsonbBuilder.create();
    static final Type SHAPE_LIST = new ArrayList<Shape>() {}.getClass().getGenericSuperclass();


    @JsonbTypeDeserializer(PolymorphicDeserializer.class)
    @JsonbTypeAlias(name = "{isCircle}", type = Circle.class)
    @JsonbTypeAlias(name = "{isSquare}", type = Square.class)
    public interface Shape {
        @SuppressWarnings("unused")
        static boolean isCircle(JsonObject value) {
            return value.containsKey("radius");
        }

        @SuppressWarnings("unused")
        static boolean isSquare(JsonObject value) {
            return value.containsKey("side");
        }
    }

    @Accessors(chain = true)
    public static @Data class Circle implements Shape {
        double radius;
    }

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

    @Test void shouldFailToDeserializeUnknownTypeField() {
        Throwable throwable = catchThrowable(() ->
            JSONB.fromJson("[{\"sides\":3}]", SHAPE_LIST)
        );

        then(throwable).isInstanceOf(JsonbException.class)
            .hasMessage("unknown shape type 'default'");
    }
}
