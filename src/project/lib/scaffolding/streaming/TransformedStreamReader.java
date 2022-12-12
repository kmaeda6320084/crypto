package project.lib.scaffolding.streaming;

import java.io.IOException;

import project.lib.scaffolding.collections.Sequence;

class TransformedStreamReader implements StreamReader {
    TransformedStreamReader(StreamReader reader, Transformer transformer) {
        this.reader = reader;
        this.transformer = transformer;
    }

    private final StreamReader reader;
    private final Transformer transformer;

    @Override
    public void advance(int consumed) throws IOException {
        transformer.advance(consumed);
    }

    @Override
    public Sequence<byte[]> read(int least) throws IOException {
        final var transformer = this.transformer;
        final var writer = transformer.writer();
        while (least > transformer.rest()) {
            final var len = Math.max(transformer.prefferedInputSize(), least);
            final var raw = reader.read(len);
            final var completed = reader.completed();
            writer.write(raw, completed);
            if (completed) {
                break;
            }
        }
        return transformer.read();
    }

    @Override
    public boolean completed() {
        return transformer.completed();
    }

}