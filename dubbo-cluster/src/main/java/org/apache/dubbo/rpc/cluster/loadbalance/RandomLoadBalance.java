/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * random load balance.
 */
public class RandomLoadBalance extends AbstractLoadBalance {

    public static final String NAME = "random";

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        // Number of invokers
        // 方法提供者总数
        int length = invokers.size();
        // Every invoker has the same weight?
        // 是否均衡权重
        boolean sameWeight = true;
        // the weight of every invokers
        // 每个方法提供者权重记录
        int[] weights = new int[length];
        // the first invoker's weight
        // 第一个服务提供者的权重
        int firstWeight = getWeight(invokers.get(0), invocation);
        weights[0] = firstWeight;
        // The sum of weights
        int totalWeight = firstWeight;
        for (int i = 1; i < length; i++) {
            // 第 i 个服务提供者的权重
            int weight = getWeight(invokers.get(i), invocation);
            // save for later use
            // 记录
            weights[i] = weight;
            // Sum
            // 累加所有权重
            totalWeight += weight;
            if (sameWeight && weight != firstWeight) {
                // 将均衡权重标记置false
                sameWeight = false;
            }
        }
        if (totalWeight > 0 && !sameWeight) {
            // 需要根据权重筛选
            // If (not every invoker has the same weight & at least one invoker's weight>0), select randomly based on totalWeight.
            int offset = ThreadLocalRandom.current().nextInt(totalWeight);
            // Return a invoker based on the random value.
            for (int i = 0; i < length; i++) {
                // 计算出随机出的目标坐落与哪个服务提供者的权重中
                offset -= weights[i];
                if (offset < 0) {
                    return invokers.get(i);
                }
            }
        }
        // If all invokers have the same weight value or totalWeight=0, return evenly.
        // 所有服务提供者权重相同，随机筛选
        return invokers.get(ThreadLocalRandom.current().nextInt(length));
    }

}
