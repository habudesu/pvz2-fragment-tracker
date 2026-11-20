# R8 规则：Release 构建开启混淆与资源压缩后使用

# kotlinx.serialization：保留生成的序列化器与伴生对象
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.pvz2tool.**$$serializer { *; }
-keepclassmembers class com.pvz2tool.** {
    *** Companion;
}
-keepclasseswithmembers class com.pvz2tool.** {
    kotlinx.serialization.KSerializer serializer(...);
}
