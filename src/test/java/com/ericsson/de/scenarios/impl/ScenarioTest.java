package com.ericsson.de.scenarios.impl;

import static java.lang.Thread.currentThread;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.de.scenarios.impl.RxApi.fromDataRecords;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.DiscreteDomain.integers;
import static com.google.common.collect.Lists.newArrayList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Named;

import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.de.scenarios.Node;
import com.ericsson.de.scenarios.api.BasicDataRecord;
import com.ericsson.de.scenarios.api.DataRecord;
import com.ericsson.de.scenarios.api.DataRecordWrapper;
import com.ericsson.de.scenarios.api.DataSource;
import com.ericsson.de.scenarios.api.TestStep;
import com.ericsson.de.scenarios.impl.Internals.VUser;
import com.ericsson.de.scenarios.impl.graph.GraphMlImporter;
import com.ericsson.de.scenarios.impl.graph.ScenarioExecutionGraph;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.base.Optional;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.Range;
import com.google.common.io.Resources;

public class ScenarioTest {

    protected static final String STORE_V_USERS_IN_CONTEXT = "scenario.debug.context.vusers.enabled";

    protected static final int NAP_TIME = 3;

    private static final Logger logger = LoggerFactory.getLogger(ScenarioTest.class);

    protected Stack<String> stack = new Stack<>();

    @Before
    public void setUp() throws Exception {
        stack.clear();
    }

    protected static void compareGraphs(ScenarioExecutionGraph actualGraph, String uri) {
        GraphMlImporter graphMlImporter = new GraphMlImporter();
        URL url = Resources.getResource("graphs/" + uri);
        ScenarioExecutionGraph expectedGraph = graphMlImporter.importFile(url);
        logger.info("Comparing graph with " + url);

        compareGraphs(actualGraph, expectedGraph);
    }

    /**
     * Please note that during graph node comparison, test step
     * names are NOT considered - test step IDs are used instead
     * (as well as Data Records, vUsers and exceptions).
     * One of the reasons for doing so have been Runnable test steps,
     * whose names include line of code where these test steps are
     * referenced from, so upon each change to the test class these
     * lines and, consequently, test step names change as well.
     *
     * @see ScenarioExecutionGraph.GraphNode#equals(Object)
     */
    protected static void compareGraphs(ScenarioExecutionGraph actual, ScenarioExecutionGraph expected) {

        // vertex set is ordered - no sorting is required
        List<ScenarioExecutionGraph.GraphNode> actualVertexes = newArrayList(actual.vertexSet());
        List<ScenarioExecutionGraph.GraphNode> expectedVertexes = newArrayList(expected.vertexSet());
        assertThat(expectedVertexes).hasSameSizeAs(actualVertexes);
        int i = 0;
        for (ScenarioExecutionGraph.GraphNode actualVertex : actualVertexes) {
            // this assertion is required to be able to see the diff
            assertThat(actualVertex).isEqualTo(expectedVertexes.get(i++));
        }

        assertThat(actual.vertexSet()).hasSameElementsAs(expected.vertexSet());
        assertThat(actual.edgeSet()).hasSameElementsAs(expected.edgeSet());
    }

    <T> T fromJson(String jsonString) throws java.io.IOException {
        ObjectReader reader = new ObjectMapper().reader(Object.class);
        return reader.readValue(jsonString);
    }

    static DataRecordWrapper getDataRecords(String ds_name, String value) {
        DataRecord basicDataRecord = BasicDataRecord.fromValues(ds_name, value);
        return getDataRecords(ds_name, basicDataRecord);
    }

    static DataRecordWrapper getDataRecords(String ds_name, DataRecord dataRecord) {
        return new DataRecords.Single(ds_name, new DefaultDataRecordTransformer(), 0, dataRecord);
    }

    @SuppressWarnings("unchecked")
    <T> T input(DataRecordWrapper dataRecord, String field) {
        return (T) input(dataRecord, field, Object.class);
    }

    <T> T input(DataRecordWrapper dataRecord, String field, Class<T> type) {
        Optional<T> fieldValue = dataRecord.getFieldValue(field, type);

        checkArgument(fieldValue.isPresent());

        return fieldValue.get();
    }

    protected DataSource<Node> getNodeDataSource() {
        return fromDataRecords("nodes", getNode("SGSN-14B", "LTE01ERB", 80), getNode("SGSN-MME", "ERBS", 20));
    }

    protected Node getNode(String networkElementId, String nodeType, Integer port) {
        return BasicDataRecord.builder().setField(Node.NETWORK_ELEMENT_ID, networkElementId).setField(Node.NODE_TYPE, nodeType)
                .setField(Node.PORT, port).build(Node.class);
    }

    public static Iterable<Integer> numbers(int count) {
        return ContiguousSet.create(Range.closed(1, count), integers());
    }

    /*---------------- Test Steps ----------------*/

    public static TestStep nop() {
        return named("No Operation");
    }

    public static TestStep named(String name) {
        return new TestStep(name) {
            @Override
            protected Optional<Object> doRun(DataRecordWrapper dataRecord) throws Exception {
                return Optional.absent();
            }

            @Override
            protected TestStep copySelf() {
                return this;
            }
        };
    }

    public static Runnable sleepy(String name) {
        return new Sleepy(name);
    }

    protected static TestStep print(String name) {
        return new Printer(name);
    }

    private static class Printer extends TestStep {
        Printer(String name) {
            super(name);
        }

        @Override
        public Optional<Object> doRun(DataRecordWrapper dataRecord) {
            System.out.println("running test step " + getName());
            return Optional.absent();
        }

        @Override
        protected TestStep copySelf() {
            return new Printer(name);
        }
    }

    public static class Sleepy implements Runnable {

        public static final int NAP_TIME = 3;

        private String name;

        public Sleepy(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(NAP_TIME * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.printf("running test step %s on a thread %s at %tT:%<tL%n", name, currentThread().getName(), new Date());
        }
    }

    public static class InlineInvocation extends TestStep {

        public InlineInvocation() {
            super("InlineInvocation");
        }

        public Optional<Object> doRun(DataRecordWrapper dataRecord) throws Exception {
            Method declaredMethod = getMethod();
            declaredMethod.setAccessible(true);

            Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
            Object[] arguments = new Object[parameterTypes.length];

            for (int i = 0; i < parameterTypes.length; i++) {
                String name = getParameterName(declaredMethod, i);
                Optional<?> argument = dataRecord.getFieldValue(name, parameterTypes[i]);
                checkArgument(argument.isPresent(), "Unable to satisfy parameter `" + name + "` of type" + parameterTypes[i]);
                arguments[i] = argument.get();
            }

            return Optional.fromNullable(declaredMethod.invoke(this, arguments));
        }

        @Override
        public String getName() {
            return getMethod().getName();
        }

        @Override
        protected TestStep copySelf() {
            final InlineInvocation parent = this;

            return new InlineInvocation() {
                @Override
                public Optional<Object> doRun(DataRecordWrapper dataRecord) throws Exception {
                    return parent.doRun(dataRecord);
                }

                @Override
                public String getName() {
                    return parent.getName();
                }
            };
        }

        private Method getMethod() {
            Method[] declaredMethods = this.getClass().getDeclaredMethods();
            checkArgument(declaredMethods.length == 1, "InlineInvocation should have only one method");
            return declaredMethods[0];
        }

        private String getParameterName(Method declaredMethod, int parameterI) {
            Annotation[] annotations = declaredMethod.getParameterAnnotations()[parameterI];
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(Named.class)) {
                    return Named.class.cast(annotation).value();
                }
            }

            throw new IllegalArgumentException("One of parameters does not have javax.inject.Named annotation in " + declaredMethod.getName());
        }
    }

    public static class Counter extends TestStep {

        private AtomicInteger count = new AtomicInteger();

        public Counter() {
            super("counter");
        }

        public Counter(String name) {
            super(name);
        }

        @Override
        protected Optional<Object> doRun(DataRecordWrapper dataRecord) throws Exception {
            count.incrementAndGet();
            return Optional.absent();
        }

        @Override
        protected TestStep copySelf() {
            return new Counter(name);
        }

        public Integer getCount() {
            return count.get();
        }

        public void assertEqualTo(Integer value) {
            assertThat(count.get()).isEqualTo(value);
        }
    }

    public TestStep pushToStack(final String a) {
        return new TestStep("pushToStack-" + a) {
            @Override
            protected Optional<Object> doRun(DataRecordWrapper dataRecord) throws Exception {
                stack.push(a);
                return Optional.absent();
            }

            @Override
            protected TestStep copySelf() {
                return pushToStack(a);
            }
        };
    }

    public static class ThrowException extends TestStep {

        private String dataRecordName;
        private String throwOn;
        private String vUserId;

        public ThrowException(String dataRecordName, String dataRecordValue, String vUserId) {
            super("Exception on `" + dataRecordValue + "` " + vUserId);
            this.dataRecordName = dataRecordName;
            this.throwOn = dataRecordValue;
            this.vUserId = vUserId;
        }

        @Override
        protected Optional<Object> doRun(DataRecordWrapper dataRecord) throws Exception {
            Optional<VUser> optionalVUser = dataRecord.getFieldValue("vUser", VUser.class);
            if (!optionalVUser.isPresent()) {
                return Optional.absent();
            }

            VUser vUser = optionalVUser.get();
            String dataRecordValue = dataRecord.getFieldValue(dataRecordName, String.class).get();
            if (dataRecordValue.equals(throwOn) && vUserId.equals(vUser.getId())) {
                System.out.println("Throwing exception. Take cover!");
                throw new VeryExpectedException();
            }
            return Optional.absent();
        }

        @Override
        protected TestStep copySelf() {
            return new ThrowException(dataRecordName, throwOn, vUserId);
        }

        @Override
        public String toString() {
            return toStringHelper(this).add("throwOn", throwOn).toString();
        }
    }

    public static class ThrowExceptionNow extends TestStep {

        public ThrowExceptionNow(String name) {
            super(name);
        }

        @Override
        protected Optional<Object> doRun(DataRecordWrapper dataRecord) throws Exception {
            System.out.println("running test step " + getName());
            throw new VeryExpectedException();
        }

        @Override
        protected TestStep copySelf() {
            return new ThrowExceptionNow(name);
        }
    }

    public static class VeryExpectedException extends RuntimeException {
    }
}
