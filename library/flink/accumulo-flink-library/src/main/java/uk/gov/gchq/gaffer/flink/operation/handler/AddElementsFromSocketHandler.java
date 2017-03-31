/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.gov.gchq.gaffer.flink.operation.handler;

import com.google.common.collect.Iterables;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.gchq.gaffer.accumulostore.AccumuloProperties;
import uk.gov.gchq.gaffer.accumulostore.AccumuloStore;
import uk.gov.gchq.gaffer.commonutil.iterable.CloseableIterable;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.flink.operation.AddElementsFromSocket;
import uk.gov.gchq.gaffer.graph.Graph;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.operation.impl.add.AddElements;
import uk.gov.gchq.gaffer.operation.impl.get.GetAllElements;
import uk.gov.gchq.gaffer.store.Context;
import uk.gov.gchq.gaffer.store.Store;
import uk.gov.gchq.gaffer.store.operation.handler.OperationHandler;
import uk.gov.gchq.gaffer.user.User;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

public class AddElementsFromSocketHandler implements OperationHandler<AddElementsFromSocket> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddElementsFromSocketHandler.class);

    @Override
    public Object doOperation(final AddElementsFromSocket operation, final Context context, final Store store) throws OperationException {
        doOperation(operation, (AccumuloStore) store);
        return null;
    }

    public void doOperation(final AddElementsFromSocket operation, final AccumuloStore store) throws OperationException {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        final Function<Iterable<? extends String>, Iterable<? extends Element>> elementGenerator = operation.getElementGenerator();
        final Class<Iterable<? extends Element>> returnClass = (Class) Iterable.class;
        final MapFunction<String, Iterable<? extends Element>> mapper =
                csv -> elementGenerator.apply(Collections.singleton(csv));

        env.socketTextStream(operation.getHostname(), operation.getPort(), "\n")
                .map(mapper)
                .returns(returnClass)
                .addSink(new GafferSink(store))
                .setParallelism(1);

        try {
            env.execute("Add elements from socket");
        } catch (Exception e) {
            throw new OperationException("Failed to add elements from port: " + operation.getPort(), e);
        }
    }

    @SuppressFBWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED", justification = "There are null checks that will initialise the fields")
    public static class GafferSink implements SinkFunction<Iterable<? extends Element>> {
        private static final long serialVersionUID = 1569145256866410621L;
        private final byte[] schema;
        private final AccumuloProperties storeProperties;

        private transient Graph graph;
        private transient ConcurrentLinkedQueue<Element> queue;
        private transient boolean restart;

        public GafferSink(final AccumuloStore store) {
            schema = store.getSchema().toCompactJson();
            storeProperties = store.getProperties();
        }

        @Override
        public void invoke(final Iterable<? extends Element> elements) throws Exception {
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<>();
                restart = true;
            }

            Iterables.addAll(queue, elements);
            if (restart) {
                restart = false;
                new Thread(() -> {
                    try {
                        if (null == graph) {
                            graph = new Graph.Builder()
                                    .storeProperties(storeProperties)
                                    .addSchema(schema)
                                    .build();
                        }

                        final Iterable<Element> wrappedQueue = new Iterable<Element>() {
                            private boolean singleIteratorInUse = false;

                            @Override
                            public Iterator<Element> iterator() {
                                if (singleIteratorInUse) {
                                    throw new RuntimeException("Only 1 iterator can be used at a time");
                                }
                                singleIteratorInUse = true;
                                return new Iterator<Element>() {
                                    @Override
                                    public boolean hasNext() {
                                        //TODO: remove the thread.sleep
                                        try {
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        return !queue.isEmpty();
                                    }

                                    @Override
                                    public Element next() {
                                        if (queue.isEmpty()) {
                                            throw new NoSuchElementException("No more elements");
                                        }

                                        return queue.poll();
                                    }
                                };
                            }
                        };

                        graph.execute(new AddElements.Builder()
                                        .input(wrappedQueue)
                                        .build(),
                                new User());

                        restart = true;

                        // TODO: remove these logs
                        LOGGER.info("Finished adding batch of elements");
                        final CloseableIterable<? extends Element> results = graph.execute(new GetAllElements(), new User());
                        LOGGER.info("All elements in graph:");
                        for (final Element result : results) {
                            LOGGER.info("Element = " + result);
                        }
                        LOGGER.info("");
                    } catch (OperationException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            }
        }
    }
}
