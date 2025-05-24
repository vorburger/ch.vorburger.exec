/*
 * #%L
 * ch.vorburger.exec
 * %%
 * Copyright (C) 2012 - 2023 Michael Vorburger
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ch.vorburger.exec;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Var;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * OutputStream "Multiplexer" which delegates to a list of other registered OutputStreams.
 *
 * <p>It's kinda like UNIX "tee". Forwarding is in the order the delegates are added. The
 * implementation is synchronous, so the added OutputStreams should be "fast" in order not to block
 * each other.
 *
 * <p>Exceptions thrown by added OutputStreams are handled gracefully: They at first do not prevent
 * delegating to the other registered OutputStreams, but then are rethrown after we've pushed to
 * delegates (possibly containing multiple causes).
 *
 * @author Michael Vorburger
 */
public class MultiOutputStream extends OutputStream {

    protected final List<OutputStream> streams = new ArrayList<>();

    public MultiOutputStream() {}

    public MultiOutputStream(OutputStream... delegates) {
        for (OutputStream delegate : delegates) {
            streams.add(delegate);
        }
    }

    @CanIgnoreReturnValue
    public synchronized MultiOutputStream addOutputStream(OutputStream delegate) {
        streams.add(delegate);
        return this;
    }

    @CanIgnoreReturnValue
    public synchronized MultiOutputStream removeOutputStream(OutputStream delegate) {
        streams.remove(delegate);
        return this;
    }

    @Override
    public void write(int b) throws IOException {
        @Var MultiCauseIOException mex = null;
        for (OutputStream stream : streams) {
            try {
                stream.write(b);
            } catch (IOException e) {
                if (mex == null) {
                    mex = new MultiCauseIOException();
                }
                mex.add("MultiOutputStream write(int b) delegation failed", e);
            }
        }
        if (mex != null) {
            throw mex;
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        @Var MultiCauseIOException mex = null;
        for (OutputStream stream : streams) {
            try {
                stream.write(b);
            } catch (IOException e) {
                if (mex == null) {
                    mex = new MultiCauseIOException();
                }
                mex.add("MultiOutputStream write(byte[] b) delegation failed", e);
            }
        }
        if (mex != null) {
            throw mex;
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        @Var MultiCauseIOException mex = null;
        for (OutputStream stream : streams) {
            try {
                stream.write(b, off, len);
            } catch (IOException e) {
                if (mex == null) {
                    mex = new MultiCauseIOException();
                }
                mex.add("MultiOutputStream write(byte[] b, int off, int len) delegation failed", e);
            }
        }
        if (mex != null) {
            throw mex;
        }
    }

    @Override
    public void flush() throws IOException {
        @Var MultiCauseIOException mex = null;
        for (OutputStream stream : streams) {
            try {
                stream.flush();
            } catch (IOException e) {
                if (mex == null) {
                    mex = new MultiCauseIOException();
                }
                mex.add("MultiOutputStream flush() delegation failed", e);
            }
        }
        if (mex != null) {
            throw mex;
        }
    }

    @Override
    public void close() throws IOException {
        @Var MultiCauseIOException mex = null;
        for (OutputStream stream : streams) {
            try {
                stream.close();
            } catch (IOException e) {
                if (mex == null) {
                    mex = new MultiCauseIOException();
                }
                mex.add("MultiOutputStream close() delegation failed", e);
            }
        }
        if (mex != null) {
            throw mex;
        }
    }
}
