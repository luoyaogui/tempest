/*
 * The MIT License (MIT)
 * Copyright (c) 2016 DataRank, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 */

package com.simplymeasured.elasticsearch.plugins.tempest.balancer

import org.elasticsearch.cluster.node.DiscoveryNodeFilters
import org.elasticsearch.cluster.node.DiscoveryNodeFilters.OpType.AND
import org.elasticsearch.cluster.node.DiscoveryNodeFilters.OpType.OR
import org.elasticsearch.cluster.routing.allocation.decider.FilterAllocationDecider
import org.elasticsearch.cluster.routing.allocation.decider.FilterAllocationDecider.*
import org.elasticsearch.common.settings.Settings

/**
 * Partial implementation of org.elasticsearch.cluster.routing.allocation.decider.FilterAllocationDecider
 *
 * Note: Index awareness is not not implemented. Node awareness should be fully supported.
 */
class MockFilterAllocationDecider(settings: Settings) : MockDecider {
    private val clusterRequireFilters: DiscoveryNodeFilters?

    private val clusterIncludeFilters: DiscoveryNodeFilters?
    private val clusterExcludeFilters: DiscoveryNodeFilters?
    init {
        clusterRequireFilters = settings.getByPrefix(CLUSTER_ROUTING_REQUIRE_GROUP).getAsMap().let { if (it.isEmpty()) null else DiscoveryNodeFilters.buildFromKeyValue(AND, it) }
        clusterIncludeFilters = settings.getByPrefix(CLUSTER_ROUTING_INCLUDE_GROUP).getAsMap().let { if (it.isEmpty()) null else DiscoveryNodeFilters.buildFromKeyValue(OR, it) }
        clusterExcludeFilters = settings.getByPrefix(CLUSTER_ROUTING_EXCLUDE_GROUP).getAsMap().let { if (it.isEmpty()) null else DiscoveryNodeFilters.buildFromKeyValue(OR, it) }
    }

    override fun canAllocate(shard: ModelShard, destNode: ModelNode): Boolean {
        if (clusterExcludeFilters?.match(destNode.backingNode.node())   ?: false) { return false }
        if (!(clusterIncludeFilters?.match(destNode.backingNode.node()) ?: true)) { return false }
        if (!(clusterRequireFilters?.match(destNode.backingNode.node()) ?: true)) { return false }
        return true
    }

    override fun canMove(shard: ModelShard, destNode: ModelNode, moves: Collection<MoveAction>): Boolean {
        return canAllocate(shard, destNode)
    }
}