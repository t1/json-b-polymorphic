package test;

import com.github.t1.polyjsonb.JsonbTypeAlias;
import com.github.t1.polyjsonb.PolymorphicDeserializer;
import lombok.Data;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;

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

@SuppressWarnings("unused")
class JsonbPolyByAliasTest {

    static final String CIRCLE_JSON = "{'type':'circle','radius':5.0}".replace('\'', '"');
    static final String SQUARE_JSON = "{'side':2.0,'type':'square'}".replace('\'', '"');
    static final String JSON = "[" + CIRCLE_JSON + "," + SQUARE_JSON + "]";
    static final Circle CIRCLE = new Circle().setRadius(5.0);
    static final Square SQUARE = new Square().setSide(2.0);
    static final List<Shape> SHAPES = asList(CIRCLE, SQUARE);

    static final Jsonb JSONB = JsonbBuilder.create();
    static final Type SHAPE_LIST = new ArrayList<Shape>() {}.getClass().getGenericSuperclass();

    @JsonbTypeDeserializer(PolymorphicDeserializer.class)
    @JsonbTypeAlias(name = "circle", type = Circle.class)
    @JsonbTypeAlias(name = "square", type = Square.class)
    public interface Shape {
        default String getType() { return getClass().getSimpleName().toLowerCase(); }
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

    @Test void shouldFailToDeserializeMissingTypeField() {
        Throwable throwable = catchThrowable(() ->
            JSONB.fromJson("[{'radius':5.0}]".replace('\'', '"'), SHAPE_LIST)
        );

        then(throwable).isInstanceOf(JsonbException.class)
            .hasMessage("unknown shape type 'default'");
    }

    @Test void shouldFailToDeserializeUnknownTypeField() {
        Throwable throwable = catchThrowable(() ->
            JSONB.fromJson("[{'type':'cube','side':13.0}]".replace('\'', '"'), SHAPE_LIST)
        );

        then(throwable).isInstanceOf(JsonbException.class)
            .hasMessage("unknown shape type 'cube'");
    }
}
