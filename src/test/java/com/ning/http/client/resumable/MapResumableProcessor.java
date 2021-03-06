/*
 * Copyright (c) 2017, 2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2010 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

package com.ning.http.client.resumable;

import com.ning.http.client.resumable.ResumableAsyncHandler.ResumableProcessor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Benjamin Hanzelmann
 */
public class MapResumableProcessor
        implements ResumableProcessor {

    Map<String, Long> map = new HashMap<>();

    public void put(String key, long transferredBytes) {
        map.put(key, transferredBytes);
    }

    public void remove(String key) {
        map.remove(key);
    }

    /**
     * NOOP
     */
    public void save(Map<String, Long> map) {

    }

    /**
     * NOOP
     */
    public Map<String, Long> load() {
        return map;
    }
}
