package com.textvision.alistclient.ui.feature.home.dto

import androidx.compose.runtime.Immutable

@Immutable
data class TaskData(
    val runningCount: Int,
    val finishedCount: Int,
    val failedBucketIds: List<String>,
    val buckets: List<TaskBucket>,
)

@Immutable
data class TaskBucket(
    val type: String,
    val running: Int,
)
