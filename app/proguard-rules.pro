# Taqvim R8 rules (T-1800, ADR-0018). Release builds use R8 full mode with code and resource shrinking; library consumer
# rules (kotlinx.serialization, Room, WorkManager, Koin, Compose, Navigation) are applied automatically. Only rules a
# library does not ship are listed here, each with the reason. `:app:verifyReleaseShrinking` checks their effect.

# Proto DataStore (T-600): protobuf-javalite reads generated message fields by name through its schema, and the
# 4.x runtime jar ships no R8 rules. Keeping the fields of the generated messages keeps stored preferences readable.
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}
