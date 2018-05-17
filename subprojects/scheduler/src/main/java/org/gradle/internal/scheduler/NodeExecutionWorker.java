/*
 * Copyright 2018 the original author or authors.
 *
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
 */

package org.gradle.internal.scheduler;

import org.gradle.internal.resources.ResourceLock;

import javax.annotation.Nullable;

public interface NodeExecutionWorker {
    NodeSchedulingResult schedule(Node node, @Nullable ResourceLock resourceLock);

    enum NodeSchedulingResult {
        STARTED, NO_WORKER_LEASE, NO_RESOURCE_LOCK
    }
}
