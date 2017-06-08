/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.tinkerpop.gremlin.process.traversal.step.filter;

import org.apache.tinkerpop.gremlin.process.traversal.Path;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.step.ByModulating;
import org.apache.tinkerpop.gremlin.process.traversal.step.FromToModulating;
import org.apache.tinkerpop.gremlin.process.traversal.step.PathProcessor;
import org.apache.tinkerpop.gremlin.process.traversal.step.TraversalParent;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.MutablePath;
import org.apache.tinkerpop.gremlin.process.traversal.traverser.TraverserRequirement;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalRing;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalUtil;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class PathFilterStep<S> extends FilterStep<S> implements FromToModulating, ByModulating, TraversalParent, PathProcessor {

    protected String fromLabel;
    protected String toLabel;
    private boolean isSimple;
    private TraversalRing<Object, Object> traversalRing;
    private Set<String> keepLabels;

    public PathFilterStep(final Traversal.Admin traversal, final boolean isSimple) {
        super(traversal);
        this.traversalRing = new TraversalRing<>();
        this.isSimple = isSimple;
    }

    @Override
    protected boolean filter(final Traverser.Admin<S> traverser) {
        final Path path = traverser.path().subPath(this.fromLabel, this.toLabel);
        if (this.traversalRing.isEmpty())
            return path.isSimple() == this.isSimple;
        else {
            this.traversalRing.reset();
            final Path byPath = MutablePath.make();
            path.forEach((object, labels) -> byPath.extend(TraversalUtil.applyNullable(object, this.traversalRing.next()), labels));
            return byPath.isSimple() == this.isSimple;
        }
    }

    @Override
    public Set<TraverserRequirement> getRequirements() {
        return Collections.singleton(TraverserRequirement.PATH);
    }

    public void addFrom(final String fromLabel) {
        this.fromLabel = fromLabel;
    }

    public void addTo(final String toLabel) {
        this.toLabel = toLabel;
    }

    @Override
    public String toString() {
        return StringFactory.stepString(this, this.isSimple ? "simple" : "cyclic", this.fromLabel, this.toLabel, this.traversalRing);
    }

    @Override
    public PathFilterStep<S> clone() {
        final PathFilterStep<S> clone = (PathFilterStep<S>) super.clone();
        clone.traversalRing = this.traversalRing.clone();
        return clone;
    }

    @Override
    public void setTraversal(final Traversal.Admin<?, ?> parentTraversal) {
        super.setTraversal(parentTraversal);
        this.traversalRing.getTraversals().forEach(this::integrateChild);
    }

    @Override
    public List<Traversal.Admin<Object, Object>> getLocalChildren() {
        return this.traversalRing.getTraversals();
    }

    @Override
    public void modulateBy(final Traversal.Admin<?, ?> pathTraversal) {
        this.traversalRing.addTraversal(this.integrateChild(pathTraversal));
    }

    @Override
    public void replaceLocalChild(final Traversal.Admin<?, ?> oldTraversal, final Traversal.Admin<?, ?> newTraversal) {
        int i = 0;
        for (final Traversal.Admin<?, ?> traversal : this.traversalRing.getTraversals()) {
            if (null != traversal && traversal.equals(oldTraversal)) {
                this.traversalRing.setTraversal(i, this.integrateChild(newTraversal));
                break;
            }
            i++;
        }
    }

    @Override
    public void reset() {
        super.reset();
        this.traversalRing.reset();
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^
                this.traversalRing.hashCode() ^
                Boolean.hashCode(this.isSimple) ^
                (null == this.fromLabel ? "null".hashCode() : this.fromLabel.hashCode()) ^
                (null == this.toLabel ? "null".hashCode() : this.toLabel.hashCode());
    }

    @Override
    public void setKeepLabels(final Set<String> labels) {
        this.keepLabels = labels;
    }

    @Override
    protected Traverser.Admin<S> processNextStart() {
        return PathProcessor.processTraverserPathLabels(super.processNextStart(), this.keepLabels);
    }

    @Override
    public Set<String> getKeepLabels() {
        return this.keepLabels;
    }
}
