// TARGET_BACKEND: JVM
// SKIP_KT_DUMP
// FULL_JDK

// KT-89566 Reflection: the new implementation uses a Java member view for the explicit java.util.List supertype, without reproducing
// K1's inherited fake override modality exactly.
// KOTLIN_REFLECT_DUMP_MISMATCH

interface KotlinList<T> : java.util.List<T>

interface SpecificList : KotlinList<String>
