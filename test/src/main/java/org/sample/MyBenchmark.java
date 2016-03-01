/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.sample;

import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.filter.KeyFilter;
import org.infinispan.marshall.core.JBossMarshaller;
import org.infinispan.marshall.core.MarshalledEntryFactory;
import org.infinispan.marshall.core.MarshalledEntryFactoryImpl;
import org.infinispan.marshall.core.MarshalledEntryImpl;
import org.infinispan.persistence.InitializationContextImpl;
import org.infinispan.persistence.mongodb.configuration.MongoDBStoreConfiguration;
import org.infinispan.persistence.mongodb.configuration.MongoDBStoreConfigurationBuilder;
import org.infinispan.persistence.mongodb.store.MongoDBStore;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.util.DefaultTimeService;
import org.mockito.Mockito;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

@State(value = Scope.Benchmark)
public class MyBenchmark {

    MongoDBStore<String, String> mongoStore;

    private static final int limit = 10000;

    @Setup
    public void setupWrites() {

        ConfigurationBuilder builder  = new ConfigurationBuilder();
        MongoDBStoreConfiguration configuration = new MongoDBStoreConfigurationBuilder(
                builder.persistence()).database("mongostoretest").collection("mongostoretest").port(27017).hostname("127.0.0.1").create();
        MarshalledEntryFactoryImpl entryFactory = new MarshalledEntryFactoryImpl();
        GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
        entryFactory.init(marshaller);
        InitializationContext context = new InitializationContextImpl(
                configuration, null, marshaller, new DefaultTimeService(), null, entryFactory);

        mongoStore = new MongoDBStore();
        mongoStore.init(context);

        for(int i = 0 ; i< limit; i++ ) {
            mongoStore.write(new MarshalledEntryImpl("k"+i, "v", null, marshaller));
        }

    }

    @TearDown
    public void stahpit(){
        mongoStore.stop();
    }

    @Benchmark
    public void testMethod(Blackhole blackhole) {
        AtomicInteger count = new AtomicInteger(0);
        mongoStore.process(KeyFilter.ACCEPT_ALL_FILTER, (var1, var2) -> {
            blackhole.consume(var1);
            count.incrementAndGet();
            return;
        }, ForkJoinPool.commonPool(), true, true);

        assert(limit == count.get());
    }
}
