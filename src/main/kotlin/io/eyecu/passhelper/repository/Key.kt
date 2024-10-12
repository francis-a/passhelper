package io.eyecu.passhelper.repository

@JvmInline
value class PartitionKey(val hash: String)

@JvmInline
value class SortKey(val range: String)