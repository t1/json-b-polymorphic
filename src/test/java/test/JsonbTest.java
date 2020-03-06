package test;

import lombok.Value;
import org.junit.jupiter.api.Test;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
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

import static org.assertj.core.api.BDDAssertions.then;

class JsonbTest {

    public static final String EMAIL_JSON = "{'type':'email','email':'test@somewhere.com'}".replace('\'', '"');
    public static final String PHONE_JSON = "{'type':'phone','phone':'+49721123456'}".replace('\'', '"');
    public static final String JSON = "[" + EMAIL_JSON + "," + PHONE_JSON + "]";
    public static final EMail EMAIL = new EMail("test@somewhere.com");
    public static final PhoneNumber PHONE_NUMBER = new PhoneNumber("+49721123456");
    public static final List<Contact> CONTACTS = List.of(EMAIL, PHONE_NUMBER);
    private static final Jsonb JSONB = JsonbBuilder.create();

    public static class ContactDeserializer implements JsonbDeserializer<Contact> {
        @Override public Contact deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            JsonObject value = parser.getObject();
            String type = value.getString("type", null);
            return JSONB.fromJson(value.toString(), classFor(type));
        }

        private Class<? extends Contact> classFor(String type) {
            switch (type) {
                case "email":
                    return EMail.class;
                case "phone":
                    return PhoneNumber.class;
                default:
                    throw new IllegalStateException("unknown contact type " + type);
            }
        }
    }

    @JsonbTypeDeserializer(ContactDeserializer.class)
    interface Contact {}


    public static class EMailSerializer implements JsonbSerializer<EMail> {
        @Override public void serialize(EMail obj, JsonGenerator generator, SerializationContext ctx) {
            generator
                .writeStartObject()
                .write("type", "email")
                .write("email", obj.getValue())
                .writeEnd();
        }
    }

    public static class EMailDeserializer implements JsonbDeserializer<EMail> {
        @Override public EMail deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            JsonObject value = (JsonObject) parser.getValue();
            return new EMail(value.getString("email"));
        }
    }

    @JsonbTypeSerializer(EMailSerializer.class)
    @JsonbTypeDeserializer(EMailDeserializer.class)
    public static @Value class EMail implements Contact {
        String value;
    }


    public static class PhoneNumberSerializer implements JsonbSerializer<PhoneNumber> {
        @Override public void serialize(PhoneNumber obj, JsonGenerator generator, SerializationContext ctx) {
            generator
                .writeStartObject()
                .write("type", "phone")
                .write("phone", obj.getValue())
                .writeEnd();
        }
    }

    public static class PhoneNumberDeserializer implements JsonbDeserializer<PhoneNumber> {
        @Override public PhoneNumber deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            JsonObject value = (JsonObject) parser.getValue();
            return new PhoneNumber(value.getString("phone"));
        }
    }

    @JsonbTypeSerializer(PhoneNumberSerializer.class)
    @JsonbTypeDeserializer(PhoneNumberDeserializer.class)
    public static @Value class PhoneNumber implements Contact {
        String value;
    }


    @Test void shouldSerializeList() {
        String json = JSONB.toJson(CONTACTS);

        then(json).isEqualTo(JSON);
    }

    @Test void shouldDeserializeEMail() {
        EMail eMail = JSONB.fromJson(EMAIL_JSON, EMail.class);

        then(eMail).isEqualTo(EMAIL);
    }

    @Test void shouldDeserializePhoneNumber() {
        PhoneNumber phone = JSONB.fromJson(PHONE_JSON, PhoneNumber.class);

        then(phone).isEqualTo(PHONE_NUMBER);
    }

    @Test void shouldDeserializeList() {
        Type type = new ArrayList<Contact>() {}.getClass().getGenericSuperclass();

        List<Contact> contacts = JSONB.fromJson(JSON, type);

        then(contacts).isEqualTo(CONTACTS);
    }
}
