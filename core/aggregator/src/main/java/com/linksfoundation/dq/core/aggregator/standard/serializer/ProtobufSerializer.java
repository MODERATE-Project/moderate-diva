package com.linksfoundation.dq.core.aggregator.standard.serializer;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;

import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;
import java.io.IOException;

public class ProtobufSerializer<T extends MessageLite> implements Serializer<T> {

    private final Parser<T> parser;

    public ProtobufSerializer(Parser<T> parser) {
        this.parser = parser;
    }

    @Override
    public int fixedSize() {
        return -1;
    }

    @Override
    public void serialize(DataOutput2 out, T value) throws IOException {
        byte[] bytes = value.toByteArray();
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    @Override
    public T deserialize(DataInput2 input, int available) throws IOException {
        int length = input.readInt();
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return parser.parseFrom(bytes);
    }
}