package com.textvision.alistclient.home.dto

data class TaskData(
    val runningCount: Int,
    val finishedCount: Int,
    val failedBucketIds: List<String>,
    val buckets: List<TaskBucket>,
)

data class TaskBucket(
    val type: String,
    val running: Int,
)
