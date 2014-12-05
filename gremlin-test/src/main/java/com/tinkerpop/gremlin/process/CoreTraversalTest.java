package com.tinkerpop.gremlin.process;

import com.tinkerpop.gremlin.ExceptionCoverage;
import com.tinkerpop.gremlin.LoadGraphWith;
import com.tinkerpop.gremlin.process.graph.GraphTraversal;
import com.tinkerpop.gremlin.structure.Contains;
import com.tinkerpop.gremlin.structure.Vertex;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import static com.tinkerpop.gremlin.LoadGraphWith.GraphData.MODERN;
import static org.junit.Assert.*;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
@ExceptionCoverage(exceptionClass = Traversal.Exceptions.class, methods = {
        "traversalIsLocked"
})
public class CoreTraversalTest extends AbstractGremlinProcessTest {

    @Test
    @LoadGraphWith(MODERN)
    public void shouldHavePropertyForEachRemainingBehaviorEvenWithStrategyRewrite() {
        final GraphTraversal<Vertex, Vertex> traversal = g.V().out().groupCount();
        traversal.forEachRemaining(Map.class, map -> assertTrue(map instanceof Map));
    }

    @Test
    @LoadGraphWith(MODERN)
    public void shouldNotAlterTraversalAfterTraversalBecomesLocked() {
        final GraphTraversal<Vertex, Vertex> traversal = this.g.V();
        assertTrue(traversal.hasNext());
        try {
            traversal.count().next();
            fail("Should throw: " + Traversal.Exceptions.traversalIsLocked());
        } catch (IllegalStateException e) {
            assertEquals(Traversal.Exceptions.traversalIsLocked().getMessage(), e.getMessage());
        } catch (Exception e) {
            fail("Should throw: " + Traversal.Exceptions.traversalIsLocked() + " not " + e + ":" + e.getMessage());
        }
    }

    @Test
    @LoadGraphWith(MODERN)
    public void shouldAddStartsProperly() {
        final Traversal<Object, Vertex> traversal = g.of().out().out();
        assertFalse(traversal.hasNext());
        traversal.asAdmin().addStarts(traversal.asAdmin().getTraverserGenerator(TraversalEngine.STANDARD).generateIterator(g.V(), traversal.asAdmin().getSteps().get(0), 1l));
        assertTrue(traversal.hasNext());
        int counter = 0;
        while (traversal.hasNext()) {
            traversal.next();
            counter++;
        }
        assertEquals(2, counter);

        traversal.asAdmin().addStarts(traversal.asAdmin().getTraverserGenerator(TraversalEngine.STANDARD).generateIterator(g.V(), traversal.asAdmin().getSteps().get(0), 1l));
        traversal.asAdmin().addStarts(traversal.asAdmin().getTraverserGenerator(TraversalEngine.STANDARD).generateIterator(g.V(), traversal.asAdmin().getSteps().get(0), 1l));
        counter = 0;
        while (traversal.hasNext()) {
            counter++;
            traversal.next();
        }
        assertEquals(4, counter);
        assertFalse(traversal.hasNext());
    }

    @Test
    @LoadGraphWith(MODERN)
    public void shouldTraversalResetProperly() {
        final Traversal<Object, Vertex> traversal = g.of().as("a").out().out().<Vertex>has("name", Contains.within, Arrays.asList("ripple", "lop")).as("b");
        if (new Random().nextBoolean()) traversal.asAdmin().reset();
        assertFalse(traversal.hasNext());
        traversal.asAdmin().addStarts(traversal.asAdmin().getTraverserGenerator(TraversalEngine.STANDARD).generateIterator(g.V(), traversal.asAdmin().getSteps().get(0), 1l));
        assertTrue(traversal.hasNext());
        int counter = 0;
        while (traversal.hasNext()) {
            traversal.next();
            counter++;
        }
        assertEquals(2, counter);

        if (new Random().nextBoolean()) traversal.asAdmin().reset();
        traversal.asAdmin().addStarts(traversal.asAdmin().getTraverserGenerator(TraversalEngine.STANDARD).generateIterator(g.V(), traversal.asAdmin().getSteps().get(0), 1l));
        assertTrue(traversal.hasNext());
        traversal.next();
        assertTrue(traversal.hasNext());
        traversal.asAdmin().reset();
        assertFalse(traversal.hasNext());

        traversal.asAdmin().addStarts(traversal.asAdmin().getTraverserGenerator(TraversalEngine.STANDARD).generateIterator(g.V(), traversal.asAdmin().getSteps().get(0), 1l));
        counter = 0;
        while (traversal.hasNext()) {
            counter++;
            traversal.next();
        }
        assertEquals(2, counter);

        assertFalse(traversal.hasNext());
        if (new Random().nextBoolean()) traversal.asAdmin().reset();
        assertFalse(traversal.hasNext());
    }
}
